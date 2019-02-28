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

package com.piranframework.safir.catalog;

import java.lang.reflect.Method;

/**
 * Represent a action method inside a action category which represented by {@link Category}
 *
 * @author Isa Hekmatizadeh
 */
public class ActionModel {
  private final String name;
  private final Method method;
  private final Class<?> argType;
  private final Class<?> returnType;

  public ActionModel(String name, Method method) {
    this.name = name;
    this.method = method;
    if (method.getParameterCount() >= 1)
      this.argType = method.getParameterTypes()[0];
    else
      this.argType = null;
    this.returnType = method.getReturnType();
  }

  public String getName() {
    return name;
  }

  public Method getMethod() {
    return method;
  }

  public Class<?> getArgType() {
    return argType;
  }

  public Class<?> getReturnType() {
    return returnType;
  }
}
