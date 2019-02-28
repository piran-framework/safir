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

import com.piranframework.safir.server.ServiceIdentity;

/**
 * When a Incoming request refer to an unknown service this exception thrown
 *
 * @author Isa Hekmatizadeh
 */
public class ServiceBundleNotFoundException extends SafirException {
  public ServiceBundleNotFoundException(ServiceIdentity serviceIdentity) {
    super("service with name %s not found", 404, serviceIdentity.toString());
  }
}
