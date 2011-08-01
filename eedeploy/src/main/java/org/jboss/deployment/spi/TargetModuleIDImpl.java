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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

/**
 * A TargetModuleID interface represents a unique identifier for a deployed application module. A deployable application module
 * can be an EAR, JAR, WAR or RAR file. A TargetModuleID can represent a root module or a child module. A root module
 * TargetModuleID has no parent. It represents a deployed EAR file or stand alone module. A child module TargetModuleID
 * represents a deployed sub module of a J2EE application. A child TargetModuleID has only one parent, the super module it was
 * bundled and deployed with. The identifier consists of the target name and the unique identifier for the deployed application
 * module.
 *
 * @author thomas.diesler@jboss.org
 *
 */
public class TargetModuleIDImpl implements TargetModuleID {

    private TargetModuleIDImpl parentModuleID;
    private List childModuleIDs = new ArrayList();
    private JBossTarget target;
    private String moduleID;
    private ModuleType moduleType;
    private boolean isRunning;

    /**
     * Construct a new target module
     */
    public TargetModuleIDImpl(Target target, String moduleID, TargetModuleID parentModuleID, boolean isRunning, ModuleType moduleType) {
        this.target = (JBossTarget) target;
        this.moduleID = moduleID;
        this.parentModuleID = (TargetModuleIDImpl) parentModuleID;
        this.isRunning = isRunning;
        this.moduleType = moduleType;
    }

    /**
     * True if the module is running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get this modules type.
     */
    public ModuleType getModuleType() {
        return moduleType;
    }

    // TargetModuleID interface ************************************************

    /**
     * Get the target
     *
     * @return the target
     */
    public Target getTarget() {
        return target;
    }

    /**
     * Get the module id
     *
     * @return the id
     */
    public String getModuleID() {
        return moduleID;
    }

    /**
     * The URL for a web module
     *
     * @return the url
     */
    public String getWebURL() {
        return null; // [todo] implement method
    }

    /**
     * The parent of this module
     *
     * @return the parent or null if there is no parent
     */
    public TargetModuleID getParentTargetModuleID() {
        return parentModuleID;
    }

    /**
     * Get the child modules
     *
     * @return an array of child modules or null if there are no children
     */
    public TargetModuleID[] getChildTargetModuleID() {
        TargetModuleID[] idarr = new TargetModuleID[childModuleIDs.size()];
        childModuleIDs.toArray(idarr);
        return idarr;
    }

    public void addChildTargetModuleID(TargetModuleID childModuleID) {
        childModuleIDs.add(childModuleID);
    }

    public int hashCode() {
        String hashStr = moduleType + moduleID;
        if (parentModuleID != null) {
            hashStr += parentModuleID.moduleID;
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
        String parentID = (parentModuleID != null ? parentModuleID.moduleID : null);
        return "[host=" + target.getHostName() + ",parent=" + parentID + ",id=" + moduleID + "]";
    }
}
