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

import com.piranframework.ganjex.api.ServiceContext;
import com.piranframework.safir.exception.CategoryNotFoundException;
import com.piranframework.safir.server.ServiceIdentity;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represent a ganjex service deployed to the safir node
 *
 * @author Isa Hekmatizadeh
 */
public class ServiceBundle {
  private final String name;
  private final String version;
  private final ServiceContext context;
  private final Map<String, Category> actionCategories = new ConcurrentHashMap<>();

  public ServiceBundle(ServiceIdentity serviceIdentity, ServiceContext context) {
    this.name = serviceIdentity.getName();
    this.version = serviceIdentity.getVersion();
    this.context = context;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public ServiceContext getContext() {
    return context;
  }

  public Map<String, Category> getActionCategories() {
    return actionCategories;
  }

  public void addCategory(Category category) {
    actionCategories.put(category.getName(), category);
  }

  public Category getCategory(String actionCategory) throws CategoryNotFoundException {
    Category category = actionCategories.get(actionCategory);
    if (Objects.isNull(category))
      throw new CategoryNotFoundException(actionCategory, fullName());
    return category;
  }

  public String fullName() {
    return name + "-" + version;
  }
}
