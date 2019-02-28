/*
 *  Copyright (c) 2018 Isa Hekmatizadeh.
 *
 *  This file is part of Safir.
 *
 *  Safir is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Safir is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Safir.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.piranframework.safir.server;

import com.piranframework.geev.Node;
import com.piranframework.safir.Constants;
import com.piranframework.safir.Util.ZMQUtil;
import com.piranframework.safir.catalog.ServiceBundle;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zeromq.*;
import zmq.ZError;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.piranframework.safir.Constants.DST_PROTOCOL_HEADER;
import static com.piranframework.safir.Constants.SEND_FILE_FETCH;

/**
 * @author Isa Hekmatizadeh
 */
@Component
public class AdminClient {
  static final Long FILE_CHUNK = 10000L;
  private static final Logger log = LoggerFactory.getLogger(AdminClient.class);
  private final ZContext ctx;
  private final Queue<Command> cmdQueue = new ConcurrentLinkedQueue<>();
  private final Map<ServiceIdentity, ServiceBundle> serviceCatalog;
  private final Map<ServiceIdentity, FileInfo> fileQueue = new ConcurrentHashMap<>();
  private final String FILE_CHUNK_STR;
  private ZMQ.Socket dealer;
  private Map<Node, Thread> controllerThreads = new ConcurrentHashMap<>();
  private Map<Node, Handler> controllerRunnables = new ConcurrentHashMap<>();
  @Value("${geev.myself-port}")
  private String port;
  @Value("${geev.myself-ip}")
  private String ip;
  @Value("${ganjex.service-path}")
  private String servicePath;
  private long lastSendHLT = 0;
  private AtomicInteger availableBuffer = new AtomicInteger(10);

  public AdminClient(ZContext ctx, Map<ServiceIdentity, ServiceBundle> serviceCatalog) {
    this.ctx = ctx;
    this.serviceCatalog = serviceCatalog;
    FILE_CHUNK_STR = String.valueOf(FILE_CHUNK);
  }

  public void adminConnected(Node node) {
    Handler handler = new Handler(node);
    Thread thread = new Thread(handler);
    controllerRunnables.put(node, handler);
    controllerThreads.put(node, thread);
    thread.start();
  }

  public void adminDisconnected(Node node) {
    Thread thread = controllerThreads.remove(node);
    Handler handler = controllerRunnables.remove(node);
    handler.terminate();
    try {
      thread.join();
    } catch (InterruptedException e) {
      thread.interrupt();
    }
  }

  public void command(Command command) {
    cmdQueue.add(command);
  }

  private void handleInternalCommand() {
    Command cmd = cmdQueue.poll();
    if (Objects.isNull(cmd))
      return;
    switch (cmd.getCommand()) {
      case Constants.CATALOG_CHANGED:
        sendINTR();
        break;
      case Constants.SEND_FILE_FETCH:
        sendFetch(cmd);
        break;
    }
  }

  private void sendFetch(Command cmd) {
    if (availableBuffer.getAndDecrement() > 0) {
      FileInfo fileInfo = (FileInfo) cmd.getArg();
      long offset = fileInfo.next();
      ZMsg msg = new ZMsg();
      msg.add(DST_PROTOCOL_HEADER);
      msg.add(Constants.FETCH);
      msg.add(fileInfo.getServiceName());
      msg.add(fileInfo.getServiceVersion());
      msg.add(String.valueOf(offset));
      msg.add(FILE_CHUNK_STR);
      msg.send(dealer);
    } else {
      cmdQueue.add(cmd);
      availableBuffer.incrementAndGet();
    }
  }

  private void sendINTR() {
    log.info("send INTR message");
    ZMsg msg = new ZMsg();
    msg.add(Constants.DST_PROTOCOL_HEADER);
    msg.add(Constants.INTR);
    serviceCatalog.keySet().forEach(s -> {
      msg.add(s.getName());
      msg.add(s.getVersion());
    });
    ZMQUtil.sendUntilSent(msg, dealer);
  }

  private void sendHLT() {
    if (lastSendHLT < System.currentTimeMillis() - Constants.HLT_INTERVAL) {
      ZMsg msg = new ZMsg();
      msg.add(Constants.DST_PROTOCOL_HEADER);
      msg.add(Constants.HLT);
      msg.add(Constants.SERVER);
      msg.send(dealer);
      lastSendHLT = System.currentTimeMillis();
    }
  }

  private void handleRecv() {
    ZMsg msg = ZMsg.recvMsg(dealer, ZMQ.NOBLOCK);
    if (Objects.isNull(msg))
      return;
    msg.pop();//empty frame
    ZFrame protocol = msg.pop();
    if (!protocol.streq(DST_PROTOCOL_HEADER)) { //message isn't DST version 1
      log.error("corrupted message received: protocol frame is {}, rest of message is: " +
          "{}", protocol, msg);
      return;
    }
    String command = msg.popString();
    switch (command) {
      case Constants.RINTR:
        log.info("RINTR command received");
        sendINTR();
        break;
      case Constants.ADD:
        log.info("ADD command received");
        handleAddCommand(msg);
        break;
      case Constants.REMOVE:
        log.info("REMOVE command received");
        handleRemoveCommand(msg);
        break;
      case Constants.FILE_INFO:
        log.info("FILE-INFO command received");
        handleFileInfo(msg);
        break;
      case Constants.FILE_CHUNK:
        log.info("FILE-CHUNK command received");
        handleFileChunk(msg);
    }
  }

  private void handleFileChunk(ZMsg msg) {
    String state = msg.popString();
    if (!"OK".equals(state)) {
      log.error("invalid file chunk received: {}", state);
      return;
    }
    String serviceName = msg.popString();
    String serviceVersion = msg.popString();
    int offset = Integer.valueOf(msg.popString());
    int size = Integer.valueOf(msg.popString());
    byte[] data = msg.pop().getData();
    ServiceIdentity serviceIdentity = new ServiceIdentity(serviceName, serviceVersion);
    FileInfo fileInfo = fileQueue.get(serviceIdentity);
    try {
      if (fileInfo.write(offset, data)) {
        if (fileInfo.check())
          deployNewService(fileInfo);
        else {
          int remains = fileInfo.chunksRemains();
          for (int i = 0; i < remains; i++)
            command(new Command(SEND_FILE_FETCH, fileInfo));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private void handleFileInfo(ZMsg msg) {
    String name = msg.popString();
    String version = msg.popString();
    String size = msg.popString();
    String hash = msg.popString();
    FileInfo fileInfo = fileQueue.get(new ServiceIdentity(name, version));
    fileInfo.setSize(Long.valueOf(size));
    fileInfo.setHash(hash);
    for (int i = 0; i < fileInfo.getChunkNum(); i++)
      command(new Command(Constants.SEND_FILE_FETCH, fileInfo));

  }

  private void handleAddCommand(ZMsg msg) {
    String serviceName = msg.popString();
    String serviceVersion = msg.popString();
    ZMsg check = new ZMsg();
    check.add(Constants.DST_PROTOCOL_HEADER);
    check.add(Constants.CHECK);
    check.add(serviceName);
    check.add(serviceVersion);
    ServiceIdentity serviceIdentity = new ServiceIdentity(serviceName, serviceVersion);
    fileQueue.put(serviceIdentity, new FileInfo(serviceIdentity));
    check.send(dealer);
  }

  private void handleRemoveCommand(ZMsg msg) {
    String serviceName = msg.popString();
    String serviceVersion = msg.popString();
    removeService(new ServiceIdentity(serviceName, serviceVersion));
  }

  private void deployNewService(FileInfo fileInfo) throws IOException {
    FileUtils.moveFile(new File(fileInfo.filePath()), new File(servicePath + File.separator +
        fileInfo.getServiceIdentity().toString() + ".jar"));
  }

  private void removeService(ServiceIdentity serviceIdentity) {
    if (!new File(servicePath + File.separator +
        serviceIdentity.toString() + ".jar").delete())
      log.error("could not delete service {}", serviceIdentity);
  }

  public class Handler implements Runnable {
    private final Node node;
    private volatile boolean stopped = false;

    public Handler(Node node) {
      this.node = node;
    }

    public void terminate() {
      this.stopped = true;
    }

    @Override
    public void run() {
      dealer = ctx.createSocket(ZMQ.DEALER);
      dealer.setIdentity((ip + ":" + port).getBytes());
      dealer.setReconnectIVLMax(1000);
      dealer.setSndHWM(1000);
      dealer.setRcvHWM(1000);
      dealer.connect(ZMQUtil.endpoint(node));
      sendINTR();
      while (!stopped) {
        try {
          sendHLT();
          handleRecv();
          handleInternalCommand();
        } catch (ZError.IOException e) {
          log.warn("Dastoor socket closed by interrupt");
        } catch (ZMQException e) {
          if (e.getErrorCode() == ZError.ETERM)
            return;
          else {
            log.error("Unexpected error occurred: ", e);
          }
        }
      }
//    try {
      dealer.disconnect(ZMQUtil.endpoint(node));
//    } catch (Exception e) {
//      log.warn("error on closing socket", e);
//    }
    }
  }
}
