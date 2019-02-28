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
import com.piranframework.safir.Util.ZMQUtil;
import com.piranframework.safir.catalog.ServiceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zeromq.*;
import zmq.ZError;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.piranframework.safir.Constants.*;

/**
 * This class responsibility is to create a zeroMQ router socket and a thread listen to that
 * socket. All the communications to zmq socket should be done by the hand of this class and its
 * internal thread. so the thread handle protocol level messages for example answer PING with
 * PONG and RINTR by INTR, and also create {@link Request} instances from incoming REQ message
 * and put them into receivedMsq queue.
 * <p>
 * This class also provide facilities to response to channels.
 *
 * @author Isa Hekmatizadeh
 */
@Component
public class SafirServer {
  private static final Logger log = LoggerFactory.getLogger(SafirServer.class);
  private final ZContext ctx;
  private final Map<ServiceIdentity, ServiceBundle> serviceCatalog;
  private final Queue<ZMsg> msgQueue = new ConcurrentLinkedQueue<>();
  private final Queue<Command> cmdQueue = new ConcurrentLinkedQueue<>();
  private final BlockingQueue<Request> receivedMsg;
  private final Set<ZFrame> channels = new HashSet<>();
  private ZMQ.Socket router;
  @Value("${geev.myself-port}")
  private String port;
  @Value("${geev.myself-ip}")
  private String ip;


  @Autowired
  public SafirServer(ZContext ctx,
                     Map<ServiceIdentity, ServiceBundle> serviceCatalog,
                     BlockingQueue<Request> receivedMsg) {
    this.ctx = ctx;
    this.serviceCatalog = serviceCatalog;
    this.receivedMsg = receivedMsg;
    new Thread(this::handle).start();
  }

  /**
   * Send a message by router socket. This method just put the message in the internal msgQueue
   * and internal thread poll it and send it via socket.
   *
   * @param msg message to send
   */
  public void send(ZMsg msg) {
    msgQueue.add(msg);
  }

  /**
   * send internal command like JOIN and LEFT to connect or disconnect zmq socket to the channels
   * node
   * <p>
   * Actual processing of the internal command should be done by the internal thread so this
   * method just put the command in the internal cmdQueue for future processing.
   *
   * @param command internal command
   */
  void command(Command command) {
    cmdQueue.add(command);
  }

  private void handle() {
    router = ctx.createSocket(ZMQ.ROUTER);
    router.setIdentity((ip + ":" + port).getBytes());
    router.setReconnectIVLMax(1000);
    router.setSndHWM(1000);
    router.setRcvHWM(1000);
    while (!Thread.currentThread().isInterrupted()) {
      try {
        handleCommand();
        handleMsg();
        handleRecv();
      } catch (ZMQException e) {
        if (e.getErrorCode() == ZError.ETERM)
          break;
        else {
          log.error("Unexpected error occurred: ", e);
        }
      }
    }
    router.close();
  }

  private void handleRecv() {
    ZMsg msg = ZMsg.recvMsg(router, ZMQ.NOBLOCK);
    if (msg == null)
      return; // no message received
    ZFrame initiator = msg.unwrap(); // sender identity
    ZFrame protocol = msg.pop();
    if (!protocol.streq(PROTOCOL_HEADER)) { //message isn't SADA version 1
      log.error("corrupted message received from {}: protocol frame is {}, rest of message is: " +
          "{}", initiator, protocol, msg);
      return;
    }
    channels.add(initiator);
    String command = msg.popString(); //command frame
    switch (command) {
      case RINTR:
        sendINTR(initiator);
        break;
      case PING:
        sendPONG(initiator);
        break;
      case REQ:
        receivedMsg.add(createRequest(msg, initiator));
        break;
      default:
        log.error("corrupted message received from {}: command frame is {}, rest of message is: " +
            "{}", initiator, command, msg);
    }
  }

  private void sendPONG(ZFrame initiator) {
    ZMsg pongMsg = new ZMsg();
    pongMsg.add(PROTOCOL_HEADER);
    pongMsg.add(PONG);
    pongMsg.wrap(initiator);
    pongMsg.send(router);
  }


  private void handleMsg() {
    ZMsg msg = msgQueue.poll();
    if (msg != null)
      msg.send(router);
  }

  private void handleCommand() {
    Command cmd = cmdQueue.poll();
    if (cmd != null) {
      switch (cmd.getCommand()) {
        case JOIN:
          Node joined = (Node) cmd.getArg();
          String identity = joined.getIp() + ":" + joined.getPort();
          router.connect(ZMQUtil.endpoint(joined));
          router.setRouterMandatory(true);
          sendINTR(new ZFrame(identity));
          break;
        case LEAVE:
          router.disconnect(ZMQUtil.endpoint((Node) cmd.getArg()));
          break;
        case CATALOG_CHANGED:
          channels.forEach(this::sendINTR);
          break;
      }
    }
  }

  private void sendINTR(ZFrame identity) {
    ZMsg intrMsg = new ZMsg();
    intrMsg.add(PROTOCOL_HEADER);
    intrMsg.add(INTR);
    serviceCatalog.keySet().forEach(s -> {
      intrMsg.add(s.getName());
      intrMsg.add(s.getVersion());
    });
    intrMsg.wrap(identity);
    ZMQUtil.sendUntilSent(intrMsg, router);
  }

  /**
   * create a {@link Request} object from incoming message
   *
   * @param msg       rest of the incoming message
   * @param initiator frame indicate the sender identification
   * @return newly created request instance
   */
  private Request createRequest(ZMsg msg, ZFrame initiator) {
    return new Request().
        setInitiator(initiator)
        .setRequestId(msg.popString())
        .setServiceName(msg.popString())
        .setServiceVersion(msg.popString())
        .setActionCategory(msg.popString())
        .setActionName(msg.popString())
        .setPayload(msg.pop().getData());
  }
}
