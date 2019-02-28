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

import com.piranframework.geev.GeevHook;
import com.piranframework.geev.Node;
import com.piranframework.geev.NodeJoined;
import com.piranframework.geev.NodeLeft;
import org.springframework.beans.factory.annotation.Autowired;

import static com.piranframework.safir.Constants.*;

/**
 * Geev Hook component to handle events of joining new node and leaving existing node
 *
 * @author Isa Hekmatizadeh
 */
@GeevHook
public class DiscoveryService {
  private final SafirServer server;
  private final AdminClient adminClient;

  @Autowired
  public DiscoveryService(SafirServer server, AdminClient adminClient) {
    this.server = server;
    this.adminClient = adminClient;
  }

  /**
   * when new node joined,  if node role is CHANNEL send JOIN command to {@link SafirServer}
   * and if node role is ADMIN call {@link AdminClient#adminConnected(Node)}
   *
   * @param node newly joined node
   */
  @NodeJoined
  public void join(Node node) {
    if (node.getRole().equals(CHANNEL))
      server.command(new Command(JOIN, node));
    else if (node.getRole().equals(ADMIN))
      adminClient.adminConnected(node);

  }

  /**
   * when a existing node left, if node role is CHANNEL send LEFT command to {@link SafirServer}
   * and if node role is ADMIN call {@link AdminClient#adminDisconnected(Node)}
   *
   * @param node disappeared node
   */
  @NodeLeft
  public void leave(Node node) {
    if (node.getRole().equals(CHANNEL))
      server.command(new Command(LEAVE, node));
    else if (node.getRole().equals(ADMIN))
      adminClient.adminDisconnected(node);
  }
}
