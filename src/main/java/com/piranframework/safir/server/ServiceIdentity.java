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

import java.util.Objects;

/**
 * Immutable Service Identification POJO. represent ganjex services identification
 *
 * @author Isa Hekmatizadeh
 */
public class ServiceIdentity {
  private final String name;
  private final String version;

  public ServiceIdentity(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public String getName() {
    return name;
  }


  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return name + "-" + version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ServiceIdentity that = (ServiceIdentity) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version);
  }
}
