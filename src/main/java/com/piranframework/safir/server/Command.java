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

/**
 * Immutable internal Command used to notify {@link SafirServer} to change behavior. commands
 * typically are changed in known proximity nodes
 *
 * @author Isa Hekmatizadeh
 */
public class Command {
  private final String command;
  private final Object arg;

  public Command(String command, Object arg) {
    this.command = command;
    this.arg = arg;
  }

  public String getCommand() {
    return command;
  }

  public Object getArg() {
    return arg;
  }
}
