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
package org.jboss.as.ee.deployment.spi;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

import static org.jboss.as.ee.deployment.spi.DeploymentMessages.MESSAGES;

/**
 * A TargetModuleID interface represents a unique identifier for a deployed application module. A deployable application module
 * can be an EAR, JAR, WAR or RAR file. A TargetModuleID can represent a root module or a child module. A root module
 * TargetModuleID has no parent. It represents a deployed EAR file or stand alone module. A child module TargetModuleID
 * represents a deployed sub module of a J2EE application. A child TargetModuleID has only one parent, the super module it was
 * bundled and deployed with. The identifier consists of the target name and the unique identifier for the deployed application
 * module.
 *
 * @author Thomas.Diesler@jboss.com
 *
 */
final class TargetModuleIDImpl implements JBossTargetModuleID {

    private final JBossTarget target;
    private final String moduleID;
    private final TargetModuleID parentModuleID;
    private final ModuleType moduleType;
    private List<TargetModuleID> childModuleIDs = new ArrayList<TargetModuleID>();
    private boolean isRunning;

    TargetModuleIDImpl(JBossTarget target, String moduleID, TargetModuleID parentModuleID, ModuleType moduleType) {
        if (target == null)
            throw new IllegalArgumentException(MESSAGES.nullArgument("target"));
        if (moduleID == null)
            throw new IllegalArgumentException(MESSAGES.nullArgument("moduleID"));
        if (moduleType == null)
            throw new IllegalArgumentException(MESSAGES.nullArgument("moduleType"));
        this.target = target;
        this.moduleID = moduleID;
        this.parentModuleID = parentModuleID;
        this.moduleType = moduleType;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    @Override
    public ModuleType getModuleType() {
        return moduleType;
    }

    void addChildTargetModuleID(TargetModuleID childModuleID) {
        childModuleIDs.add(childModuleID);
    }

    // TargetModuleID interface ************************************************

    @Override
    public Target getTarget() {
        return target;
    }

    @Override
    public String getModuleID() {
        return moduleID;
    }

    @Override
    public String getWebURL() {
        return null; // [todo] implement method
    }

    @Override
    public TargetModuleID getParentTargetModuleID() {
        return parentModuleID;
    }

    @Override
    public TargetModuleID[] getChildTargetModuleID() {
        TargetModuleID[] idarr = new TargetModuleID[childModuleIDs.size()];
        childModuleIDs.toArray(idarr);
        return idarr;
    }

    public int hashCode() {
        String hashStr = moduleType + moduleID;
        if (parentModuleID != null) {
            hashStr += parentModuleID.getModuleID();
        }
        return hashStr.hashCode();
    }

    /**
     * Equality is defined by moduleType, moduleID, and parentModuleID
     */
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof TargetModuleIDImpl) {
            TargetModuleIDImpl other = (TargetModuleIDImpl) obj;
            equals = moduleType.equals(other.moduleType) && moduleID.equals(other.moduleID);
            if (equals && parentModuleID != null)
                equals = equals && parentModuleID.equals(other.parentModuleID);
        }
        return equals;
    }

    public String toString() {
        String parentID = (parentModuleID != null ? parentModuleID.getModuleID() : null);
        return "[target=" + target.getName() + ",parent=" + parentID + ",type=" + moduleType+ ",id=" + moduleID + "]";
    }
}
