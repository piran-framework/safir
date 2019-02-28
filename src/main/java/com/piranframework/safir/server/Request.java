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

import org.zeromq.ZFrame;

/**
 * Each Request from channels with the type of REQ transformed to an instance of this class for
 * further processing
 *
 * @author Isa Hekmatizadeh
 */
public class Request {
  /**
   * request id used by channel to match the correlation between request and responses
   */
  private String requestId;
  private String serviceName;
  private String serviceVersion;
  private String actionCategory;
  private String actionName;
  private ZFrame initiator; //channel address frame
  private byte[] payload;

  public String getRequestId() {
    return requestId;
  }

  public Request setRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public String getServiceName() {
    return serviceName;
  }

  public Request setServiceName(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  public String getServiceVersion() {
    return serviceVersion;
  }

  public Request setServiceVersion(String serviceVersion) {
    this.serviceVersion = serviceVersion;
    return this;
  }

  public String getActionCategory() {
    return actionCategory;
  }

  public Request setActionCategory(String actionCategory) {
    this.actionCategory = actionCategory;
    return this;
  }

  public String getActionName() {
    return actionName;
  }

  public Request setActionName(String actionName) {
    this.actionName = actionName;
    return this;
  }

  public ZFrame getInitiator() {
    return initiator;
  }

  public Request setInitiator(ZFrame initiator) {
    this.initiator = initiator;
    return this;
  }

  public byte[] getPayload() {
    return payload;
  }

  public Request setPayload(byte[] payload) {
    this.payload = payload;
    return this;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("Request{");
    sb.append("requestId='").append(requestId).append('\'');
    sb.append(", serviceName='").append(serviceName).append('\'');
    sb.append(", serviceVersion='").append(serviceVersion).append('\'');
    sb.append(", actionCategory='").append(actionCategory).append('\'');
    sb.append(", actionName='").append(actionName).append('\'');
    sb.append(", initiator=").append(initiator);
    sb.append('}');
    return sb.toString();
  }
}
