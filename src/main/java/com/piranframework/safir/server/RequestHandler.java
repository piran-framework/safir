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

import com.piranframework.safir.Constants;
import com.piranframework.safir.catalog.ActionModel;
import com.piranframework.safir.catalog.Category;
import com.piranframework.safir.catalog.ServiceBundle;
import com.piranframework.safir.exception.CouldNotCastArgument;
import com.piranframework.safir.exception.ExceptionPayload;
import com.piranframework.safir.exception.SafirException;
import com.piranframework.safir.exception.ServiceBundleNotFoundException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Actual Request Handler, which process incoming REQ command, pass them to related service and
 * send back the response by {@link SafirServer} to the channel. {@link RequestHandler}
 * poll Incoming Request from a BlockingQueue named receivedMsg which is a spring bean shared
 * between RequestHandler and {@link SafirServer}. after polling request it pass the request to
 * its internal executor service to actually process them.
 *
 * @author Isa Hekmatizadeh
 */
@Component
public final class RequestHandler {
  private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
  private final Map<ServiceIdentity, ServiceBundle> services;
  private final SafirServer server;
  private final BlockingQueue<Request> receivedMsg;
  private final ExecutorService executorService = Executors.newFixedThreadPool(4);
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Constructor of the RequestHandler, after initializing fields, it creates and runs a thread
   * to poll incoming request and pass them to executor service
   *
   * @param services    services available in node
   * @param server      an instance of {@link SafirServer}
   * @param receivedMsg queue of incoming request
   */
  @Autowired
  public RequestHandler(Map<ServiceIdentity, ServiceBundle> services, SafirServer server,
                        BlockingQueue<Request> receivedMsg) {
    this.services = services;
    this.server = server;
    this.receivedMsg = receivedMsg;
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Request received = this.receivedMsg.poll(10, TimeUnit.SECONDS);
          if (Objects.nonNull(received))
            executorService.submit(() -> this.handle(received));
        } catch (InterruptedException ignored) {
        }
      }
    }).start();
  }


  private void handle(Request request) {
    try {
      try {
        byte[] response = process(request);
        response(request, 200, response);
      } catch (SafirException e) {
        log.warn("bad request with id {}: {}", request.getRequestId(), e.getMessage());
        response(request, e.getCode(), mapper.writeValueAsBytes(e.payload()));
      } catch (InvocationTargetException e) {
        log.error("service internal error for request {}:", request, e.getCause());
        ExceptionPayload payload = new ExceptionPayload().setCode(500)
            .setMessage(e.getCause().getMessage());
        response(request, 500, mapper.writeValueAsBytes(payload));
      } catch (IllegalAccessException e) {
        log.error("request {} can't be processed cause by illegal access", request.getRequestId(), e);
        response(request, 501, mapper.writeValueAsBytes(e.getMessage()));
      }
    } catch (IOException e) {
      log.error("could not send response of {}", request.getRequestId(), e);
    }
  }

  private byte[] process(Request request) throws SafirException, IOException,
      InvocationTargetException, IllegalAccessException {
    ServiceIdentity identity = new ServiceIdentity(request.getServiceName(),
        request.getServiceVersion());
    ServiceBundle serviceBundle = services.get(identity);

    if (Objects.isNull(serviceBundle))
      throw new ServiceBundleNotFoundException(identity);
    Category category = serviceBundle.getCategory(request.getActionCategory());
    ActionModel action = category.getAction(request.getActionName());
    Object result;
    if (Objects.nonNull(action.getArgType())) { //action has a argument
      try {
        Object o = mapper.readValue(request.getPayload(), action.getArgType());
        result = action.getMethod().invoke(category.getBean(), o);
      } catch (IOException e) {
        throw new CouldNotCastArgument(action.getArgType(), action.getName(), category.getName(),
            serviceBundle.fullName());
      }
    } else //action has no argument
      result = action.getMethod().invoke(category.getBean(), (Object[]) null);
    return mapper.writeValueAsBytes(result);
  }

  private void response(Request request, Integer responseCode, byte[] response) {
    ZMsg msg = new ZMsg();
    msg.add(Constants.PROTOCOL_HEADER);
    msg.add(Constants.REP);
    msg.add(request.getRequestId());
    msg.add(ByteBuffer.allocate(4).putInt(responseCode).array());
    msg.add(response);
    msg.wrap(request.getInitiator());
    server.send(msg);
  }
}
