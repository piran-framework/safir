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
public abstract class SafirException extends Exception {
  private final int code;
  private final String[] args;

  protected SafirException(String messageTempate, int code, String... args) {
    super(String.format(messageTempate, (Object[]) args));
    this.args = args;
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public String[] getArgs() {
    return args;
  }

  public ExceptionPayload payload() {
    return new ExceptionPayload()
        .setMessage(this.getMessage())
        .setArgs(args)
        .setCode(code);
  }
}
