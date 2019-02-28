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

package com.piranframework.safir;

/**
 * Constants used in Safir node.
 * specification
 *
 * @author Isa Hekmatizadeh
 */
public class Constants {
  /**
   * Protocol Header which include protocol version
   */
  public static final String PROTOCOL_HEADER = "SADA1";
  public static final String DST_PROTOCOL_HEADER = "DST1";
  /**
   * Protocol Commands
   * For the meaning of the commands see SAFIR-DARBAAN
   */
  public static final String INTR = "INTR";
  public static final String RINTR = "RINTR";
  public static final String PING = "PING";
  public static final String PONG = "PONG";
  public static final String REQ = "REQ";
  public static final String REP = "REP";

  /**
   * Internal Commands
   */
  public static final String JOIN = "JOIN";
  public static final String LEAVE = "LEAVE";

  /**
   * Node Roles
   */
  public static final String CHANNEL = "CHANNEL";
  public static final String SERVER = "SERVER";
  public static final String ADMIN = "ADMINISTRATOR";

  /**
   * Dastoor protocol constants
   */
  public static final long HLT_INTERVAL = 40000;
  public static final String HLT = "HLT";
  public static final String CATALOG_CHANGED = "CATALOG_CHANGED";
  public static final String CHECK = "CHECK";
  public static final String ADD = "ADD";
  public static final String REMOVE = "REMOVE";
  public static final String FILE_INFO = "FILE-INFO";
  public static final String SEND_FILE_FETCH = "SEND-FILE-FETCH";
  public static final String FETCH = "FETCH";
  public static final String FILE_CHUNK = "FILE-CHUNK";
}
