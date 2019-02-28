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

package com.piranframework.safir.Util;

import com.piranframework.geev.Node;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;
import zmq.ZError;

/**
 * @author Isa Hekmatizadeh
 */
public class ZMQUtil {
  public static void sendUntilSent(ZMsg msg, ZMQ.Socket socket) {
    boolean success = false;
    while (!success) {
      try {
        msg.send(socket);
      } catch (ZMQException e) {
        if (e.getErrorCode() == ZError.EHOSTUNREACH)
          continue;
      }
      success = true;
    }
  }
  public static String endpoint(Node node) {
    return "tcp://" + node.getIp() + ":" + node.getPort();
  }

}
