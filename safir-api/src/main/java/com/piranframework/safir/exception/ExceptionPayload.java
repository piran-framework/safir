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

package com.piranframework.safir.exception;

/**
 * @author Isa Hekmatizadeh
 */
public class ExceptionPayload {
  private int code;
  private String message;
  private String[] args;

  public int getCode() {
    return code;
  }

  public ExceptionPayload setCode(int code) {
    this.code = code;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public ExceptionPayload setMessage(String message) {
    this.message = message;
    return this;
  }

  public String[] getArgs() {
    return args;
  }

  public ExceptionPayload setArgs(String[] args) {
    this.args = args;
    return this;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("ExceptionPayload{");
    sb.append(code);
    sb.append(",").append(message);
    sb.append('}');
    return sb.toString();
  }
}
