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

import com.piranframework.ganjex.GanjexHook;
import com.piranframework.ganjex.api.ServiceContext;
import com.piranframework.ganjex.api.ShutdownHook;
import com.piranframework.ganjex.api.StartupHook;
import com.piranframework.safir.Constants;
import com.piranframework.safir.api.Action;
import com.piranframework.safir.api.ActionCategory;
import com.piranframework.safir.catalog.ActionModel;
import com.piranframework.safir.catalog.Category;
import com.piranframework.safir.catalog.ServiceBundle;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * @author Isa Hekmatizadeh
 */
@GanjexHook
public class ServiceLoader {
  private final Map<ServiceIdentity, ServiceBundle> services;
private final SafirServer safirServer;
private final AdminClient adminClient;
  @Autowired
  public ServiceLoader(Map<ServiceIdentity, ServiceBundle> services, SafirServer safirServer, AdminClient adminClient) {
    this.services = services;
    this.safirServer = safirServer;
    this.adminClient = adminClient;
  }

  @StartupHook
  public void serviceAdded(ServiceContext context) {
    ServiceIdentity serviceIdentity = new ServiceIdentity(context.getName(),
        String.valueOf(context.getVersion()));
    ServiceBundle bundle = new ServiceBundle(serviceIdentity, context);
    Reflections.log = LoggerFactory.getLogger(Reflections.class);
    Reflections reflections = new Reflections(new MethodAnnotationsScanner(),
        new TypeAnnotationsScanner(), new SubTypesScanner(),context.getClassLoader());
    Set<Class<?>> categories = reflections.getTypesAnnotatedWith(ActionCategory.class);
    Set<Method> actionMethods = reflections.getMethodsAnnotatedWith(Action.class);
    categories.forEach(cat -> {
      try {
        Object bean = cat.getConstructor((Class<?>[]) null).newInstance((Object[]) null);
        Category category = new Category(cat.getAnnotation(ActionCategory.class).value(), bean);
        actionMethods.stream().filter(act -> act.getDeclaringClass().equals(cat))
            .map(actm -> new ActionModel(actm.getAnnotation(Action.class).value(), actm))
            .forEach(category::addAction);
        bundle.addCategory(category);
        announce();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        e.printStackTrace();
      }
    });
    services.put(serviceIdentity, bundle);
  }

  private void announce() {
    Command command = new Command(Constants.CATALOG_CHANGED,null);
    adminClient.command(command);
    safirServer.command(command);
  }

  @ShutdownHook
  public void serviceRemoved(ServiceContext context) {
    services.remove(new ServiceIdentity(context.getName(), String.valueOf(context.getVersion())));
    announce();
  }
}
