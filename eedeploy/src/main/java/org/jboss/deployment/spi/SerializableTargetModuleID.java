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
package org.jboss.deployment.spi;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;

import javax.enterprise.deploy.spi.TargetModuleID;

/**
 * A Serializable representation of the TargetModuleID
 *
 * @author Scott.Stark@jboss.org
 *
 */
public class SerializableTargetModuleID implements Serializable {
    private static final long serialVersionUID = 6856468929226666749L;

    private SerializableTargetModuleID parentModuleID;
    private ArrayList childModuleIDs = new ArrayList();
    private String moduleID;
    private int moduleType;
    /** */
    private boolean isRunning;
    /** An optional InputStream to use to copy the contents */
    private transient InputStream contentIS;

    public SerializableTargetModuleID(TargetModuleIDImpl impl) {
        this(null, impl);
    }

    public SerializableTargetModuleID(SerializableTargetModuleID parent, TargetModuleIDImpl impl) {
        parentModuleID = parent;
        moduleID = impl.getModuleID();
        moduleType = impl.getModuleType().getValue();

        TargetModuleID[] children = impl.getChildTargetModuleID();
        int length = children != null ? children.length : 0;
        for (int n = 0; n < length; n++) {
            TargetModuleIDImpl child = (TargetModuleIDImpl) children[n];
            childModuleIDs.add(new SerializableTargetModuleID(this, child));
        }
    }

    public SerializableTargetModuleID(SerializableTargetModuleID parent, String moduleID, int moduleType, boolean isRunning) {
        parentModuleID = parent;
        this.moduleID = moduleID;
        this.moduleType = moduleType;
        this.isRunning = isRunning;
    }

    public SerializableTargetModuleID getParentModuleID() {
        return parentModuleID;
    }

    public void addChildTargetModuleID(SerializableTargetModuleID child) {
        childModuleIDs.add(child);
    }

    public void clearChildModuleIDs() {
        childModuleIDs.clear();
    }

    public SerializableTargetModuleID[] getChildModuleIDs() {
        SerializableTargetModuleID[] ids = new SerializableTargetModuleID[childModuleIDs.size()];
        childModuleIDs.toArray(ids);
        return ids;
    }

    public String getModuleID() {
        return moduleID;
    }

    public int getModuleType() {
        return moduleType;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean flag) {
        this.isRunning = flag;
    }

    /**
     * An optional deployment archive content stream for the top-level module that should be used over the moduleID url.
     *
     * @return the archive input stream if it exists
     */
    public InputStream getContentIS() {
        return contentIS;
    }

    /**
     *
     * @param contentIS
     */
    public void setContentIS(InputStream contentIS) {
        this.contentIS = contentIS;
    }

    public String toString() {
        return "SerializableTargetModuleID{" + "parentModuleID=@" + System.identityHashCode(parentModuleID) + ", childModuleIDs=" + childModuleIDs + ", moduleID='"
                + moduleID + "'" + ", moduleType=" + moduleType + ", isRunning=" + isRunning + "}";
    }

}
