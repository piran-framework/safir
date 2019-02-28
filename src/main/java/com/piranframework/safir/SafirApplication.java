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

import com.piranframework.ganjex.EnableGanjexContainer;
import com.piranframework.geev.EnableGeevContainer;
import com.piranframework.safir.catalog.ServiceBundle;
import com.piranframework.safir.server.Request;
import com.piranframework.safir.server.ServiceIdentity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.zeromq.ZContext;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main Safir spring boot application class
 *
 * @author hekmatof
 */
@SpringBootApplication
@EnableGanjexContainer
@EnableGeevContainer
public class SafirApplication {

  /**
   * starting point of the safir node
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(SafirApplication.class, args);
  }

  /**
   * @return A concurrent map represent the service catalog of the node
   */
  @Bean
  public Map<ServiceIdentity, ServiceBundle> serviceCatalog() {
    return new ConcurrentHashMap<>();
  }

  /**
   * @return A blocking queue of the messages received by the
   * {@link com.piranframework.safir.server.SafirServer} and should be processed by
   * {@link com.piranframework.safir.server.RequestHandler}
   */
  @Bean
  public BlockingQueue<Request> receivedMsg() {
    return new ArrayBlockingQueue<>(10000);
  }

  /**
   * create zeroMQ context used to create sockets
   *
   * @return zeroMQ context
   */
  @Bean
  public ZContext context() {
    ZContext ctx = new ZContext(1);
    //register shutdown hook to destroy zeroMQ context
    Runtime.getRuntime().addShutdownHook(new Thread(ctx::destroy));
    return ctx;
  }
}
