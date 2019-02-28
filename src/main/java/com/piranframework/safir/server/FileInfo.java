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

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Isa Hekmatizadeh
 */
public class FileInfo {
  private final Queue<Long> chunksToFetch = new ConcurrentLinkedQueue<>();
  private final Set<Long> chunksFetched = new HashSet<>();
  private RandomAccessFile raf;
  private long chunkNum;
  private long size;
  private String hash;
  private final ServiceIdentity serviceIdentity;

  public FileInfo(ServiceIdentity serviceIdentity) {
    this.serviceIdentity =serviceIdentity;
  }

  public String filePath() {
    return "/tmp" + File.separator + serviceIdentity.toString();
  }

  public int chunksRemains() {
    return chunksToFetch.size();
  }

  public synchronized boolean write(int offset, byte[] data) throws IOException {
    raf.seek(offset);
    raf.write(data);
    chunksFetched.add(offset / AdminClient.FILE_CHUNK);
    chunksToFetch.remove(offset / AdminClient.FILE_CHUNK);
    return chunksFetched.size() >= chunkNum;
  }

  public boolean check() throws IOException {
    raf.close();
    FileInputStream fis = new FileInputStream(filePath());
    String generatedHash = DigestUtils.sha1Hex(fis);
    if (hash.equals(generatedHash))
      return true;
    raf = new RandomAccessFile(filePath(), "rw");
    for (long i = 0; i < chunkNum; i++) {
      if (!chunksFetched.contains(i))
        chunksToFetch.add(i);
    }
    return false;
  }

  public long next() {
    if (chunksToFetch.size() > 0)
      return chunksToFetch.poll() * AdminClient.FILE_CHUNK;
    for (long i = 0; i < chunkNum; i++)
      if (!chunksFetched.contains(i)) {
        return i * AdminClient.FILE_CHUNK;
      }
    return -1;
  }

  public long getSize() {
    return size;
  }

  public FileInfo setSize(long size) {
    this.size = size;
    try {
      raf = new RandomAccessFile(filePath(), "rw");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    chunkNum = (size / AdminClient.FILE_CHUNK);
    if (size % AdminClient.FILE_CHUNK > 0)
      chunkNum++;
    for (long i = 0; i < chunkNum; i++)
      chunksToFetch.add(i);
    return this;
  }

  public String getHash() {
    return hash;
  }

  public FileInfo setHash(String hash) {
    this.hash = hash;
    return this;
  }

  public String getServiceName() {
    return serviceIdentity.getName();
  }

  public String getServiceVersion() {
    return serviceIdentity.getVersion();
  }

  public ServiceIdentity getServiceIdentity() {
    return serviceIdentity;
  }

  public long getChunkNum() {
    return chunkNum;
  }
}
