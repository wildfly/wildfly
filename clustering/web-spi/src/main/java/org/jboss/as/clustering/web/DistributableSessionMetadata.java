/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.clustering.web;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Encapsulates the replicated metadata for a session. The wrapped data can be mutated, allowing the same object to always be
 * stored in JBoss Cache. Always storing the same object avoids an earlier, no longer accurate, object being reverted into the
 * cache during a transaction rollback.
 */
public class DistributableSessionMetadata implements Externalizable {
    /** The serialVersionUID */
    private static final long serialVersionUID = -6845914023373746866L;

    private String id;
    private long creationTime;
    private int maxInactiveInterval;
    private boolean isNew;
    private boolean isValid;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.id);
        out.writeLong(this.creationTime);
        out.writeInt(this.maxInactiveInterval);
        out.writeBoolean(this.isNew);
        out.writeBoolean(this.isValid);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        this.id = in.readUTF();
        this.creationTime = in.readLong();
        this.maxInactiveInterval = in.readInt();
        this.isNew = in.readBoolean();
        this.isValid = in.readBoolean();
    }
}