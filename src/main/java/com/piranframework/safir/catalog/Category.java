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

import com.piranframework.safir.exception.ActionNotFoundException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represent a action category located in a ganjex service which represented
 * by {@link ServiceBundle}
 *
 * @author Isa Hekmatizadeh
 */
public class Category {
  private final String name;
  private final Object bean;
  private final Map<String, ActionModel> actions = new ConcurrentHashMap<>();

  public Category(String name, Object bean) {
    this.name = name;
    this.bean = bean;
  }

  public String getName() {
    return name;
  }

  public Object getBean() {
    return bean;
  }

  public Map<String, ActionModel> getActions() {
    return actions;
  }

  public void addAction(ActionModel actionModel) {
    actions.put(actionModel.getName(), actionModel);
  }

  public ActionModel getAction(String actionName) throws ActionNotFoundException {
    ActionModel actionModel = actions.get(actionName);
    if (Objects.isNull(actionModel))
      throw new ActionNotFoundException(actionName,name);
    return actionModel;
  }
}
