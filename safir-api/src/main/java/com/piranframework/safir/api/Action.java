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

package com.piranframework.safir.api;

import java.lang.annotation.*;

/**
 * Indicate that the method is an action with the name provided by value field. every Action
 * should be located in the class with the {@link ActionCategory} annotation.
 * <p>
 * Every action has non argument or just one POJO argument. every action can return void or any
 * other POJO object. remember both argument and return type of an action serialize and
 * deserialize from byte arrays with the MsgPack
 *
 * @author Isa Hekmatizadeh
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
  /**
   * @return action name
   */
  String value();
}
