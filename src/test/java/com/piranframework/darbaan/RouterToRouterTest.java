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

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.ZError;

/**
 * @author Isa Hekmatizadeh
 */
public class RouterToRouterTest {
  public static void main(String[] args) {
    ZContext ctx = new ZContext(1);
    new Thread(() -> {
      ZMQ.Socket server = ctx.createSocket(ZMQ.ROUTER);
      server.setIdentity("khar".getBytes());
      server.bind("tcp://192.168.13.70:2000");
      while (!Thread.currentThread().isInterrupted()) {
        try {
          String msg = server.recvStr();
          System.out.println("recv: " + msg);
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      server.close();
    }).start();
    new Thread(() -> {
      ZMQ.Socket client = ctx.getContext().socket(ZMQ.ROUTER);
      client.setRouterMandatory(true);
      client.setSendTimeOut(0);
      client.connect("tcp://192.168.13.70:2000");
      boolean result = false;
      while (!result) {
        try {
          result = client.sendMore("khar".getBytes());
          client.sendMore("");
          client.send("olagh");
        } catch (ZMQException e) {
          if (e.getErrorCode() == ZError.EHOSTUNREACH)
            continue;
        }
        result = true;
      }
    }).start();
    Runtime.getRuntime().addShutdownHook(new Thread(ctx::destroy));
  }
}
