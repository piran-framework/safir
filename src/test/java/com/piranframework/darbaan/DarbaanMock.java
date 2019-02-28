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

package com.piranframework.darbaan;

import com.piranframework.geev.Geev;
import com.piranframework.geev.GeevConfig;
import com.piranframework.geev.Node;
import com.piranframework.safir.Constants;
import com.piranframework.safir.server.ServiceIdentity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zeromq.*;
import zmq.ZError;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static com.piranframework.safir.Constants.*;

/**
 * @author Isa Hekmatizadeh
 */
public class DarbaanMock {
  private static final int TEST_SIZE = 1000000;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static Map<String, Boolean> servers = new HashMap<>();
  private static Map<ServiceIdentity, ZFrame> catalog = new ConcurrentHashMap<>();
  private static Queue<ZMsg> sendMessageQueue = new ConcurrentLinkedQueue<>();
  private static AtomicLong responseNum = new AtomicLong(0);
  private static AtomicLong requestId = new AtomicLong(0);
  private static volatile long start;
  private static volatile long end;
  private static Geev geev;

  public static void main(String[] args) throws IOException {
    ZContext ctx = new ZContext(1);
    geev = new Geev(new GeevConfig.Builder()
        .setMySelf(new Node(CHANNEL, args[0],
            Integer.parseInt(args[1])))
        .onJoin(DarbaanMock::join)
        .onLeave(DarbaanMock::leave)
        .build());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      geev.destroy();
      ctx.destroy();
      long duration = end - start;
      System.out.println("duration: " + duration + "ms");
      System.out.println(requestId.get() + " messages sent");
      System.out.println(responseNum.get() + " message received");
      System.out.println("tps: " + responseNum.get() * 1000 / duration);
    }));
    new Thread(() -> {
      try {
        handleMessage(ctx, args[0], Integer.parseInt(args[1]));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();
    new Thread(() -> {
      ServiceIdentity serviceIdentity = new ServiceIdentity("test", "1");
      ZFrame server = null;
      while (Objects.isNull(server)) {
        server = catalog.get(serviceIdentity);
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      System.out.println("service found");
      start = System.currentTimeMillis();
      byte[] payload = new byte[0];
      try {
        payload = mapper.writeValueAsBytes(Collections.singletonList("salam"));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      for (int i = 0; i < TEST_SIZE; i++) {
        ZMsg msg = new ZMsg();
        msg.add(Constants.PROTOCOL_HEADER);
        msg.add(Constants.REQ);
        msg.add(String.valueOf(requestId.incrementAndGet()));
        msg.add("test");
        msg.add("1");
        msg.add("testCat");
        msg.add("testAct");
        msg.add(payload);
        msg.wrap(server);
        sendMessageQueue.add(msg);
      }
    }).start();

  }

  private static void handleMessage(ZContext ctx, String ip, Integer port)
      throws InterruptedException {
    ZMQ.Socket socket = ctx.createSocket(ZMQ.ROUTER);
    socket.setSndHWM(10000);
    socket.setRcvHWM(10000);
    socket.setIdentity((ip + ":" + port).getBytes());
    socket.setRouterMandatory(true);
    String endpoint = "tcp://*:" + port;
    socket.bind(endpoint);
    Long pingInterval = System.currentTimeMillis() + 2000;
    while (!Thread.currentThread().isInterrupted()) {
      if (System.currentTimeMillis() > pingInterval) {
        servers.keySet().forEach(p -> sendPING(p, socket));
        pingInterval = System.currentTimeMillis() + 2000;
      }
      geev.allNodes(SERVER).stream().filter(n -> !servers.containsKey(identity(n)))
          .forEach(n -> servers.put(identity(n), false));
      servers.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey)
          .forEach((id) -> sendRINTR(id, socket));

      //send message
      ZMsg shouldSend = sendMessageQueue.poll();
      try {
        if (Objects.nonNull(shouldSend))
          shouldSend.send(socket, false);
        ZMsg msg = ZMsg.recvMsg(socket, ZMQ.NOBLOCK);
        if (msg != null)
          handleMessage(msg);
      } catch (ZMQException e) {
        if (e.getErrorCode() == ZError.EHOSTUNREACH) {
          System.out.println("ERROR: host not found for this message: ");
          shouldSend.dump();
        } else if (e.getErrorCode() == ZError.ETERM) {

          System.out.println("exited by ETERM");
          break;
        } else {
          throw e;
        }
      }
    }
    socket.close();
  }

  private static void handleMessage(ZMsg msg) {
    ZFrame serverIdentity = msg.unwrap();
    if (!Constants.PROTOCOL_HEADER.equals(msg.popString())) {
      System.out.println("ERROR: bad message received");
      return;
    }
    String command = msg.popString();
    switch (command) {
      case INTR:
        String serviceName = msg.popString();
        while (Objects.nonNull(serviceName)) {
          String version = msg.popString();
          ServiceIdentity serviceIdentity = new ServiceIdentity(serviceName, version);
          System.out.println("new Service found " + serviceIdentity);
          catalog.put(serviceIdentity, serverIdentity);
          serviceName = msg.popString();
        }
        break;
      case PONG:
        break;
      case REP:
        if (responseNum.incrementAndGet() == TEST_SIZE) {
          end = System.currentTimeMillis();
          System.exit(0);
        }
        break;
      default:
        System.out.println("ERROR: unknown message received");
    }
  }

  private static void sendPING(String identity, ZMQ.Socket socket) {
    try {
      ZMsg m = new ZMsg();
      m.add(PROTOCOL_HEADER);
      m.add(PING);
      m.wrap(new ZFrame(identity));
      m.send(socket);
    } catch (ZMQException e) {
      if (e.getErrorCode() != ZError.EHOSTUNREACH)
        e.printStackTrace();
    }
  }

  private static void sendRINTR(String identity, ZMQ.Socket socket) {
    try {
      ZMsg m = new ZMsg();
      m.add(PROTOCOL_HEADER);
      m.add(RINTR);
      m.wrap(new ZFrame(identity));
      m.send(socket);
      servers.put(identity, true);
    } catch (ZMQException e) {
      if (e.getErrorCode() != ZError.EHOSTUNREACH)
        e.printStackTrace();
    }
  }

  private static void leave(Node node) {
    System.out.println(node + "left");
  }

  private static void join(Node node) {
    System.out.println(node + "joined");
    if (node.getRole().equals(SERVER))
      servers.put(identity(node), false);
  }

  private static String identity(Node node) {
    return node.getIp() + ":" + node.getPort();
  }
}
