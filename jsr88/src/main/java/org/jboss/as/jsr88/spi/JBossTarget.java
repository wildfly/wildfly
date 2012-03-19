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
package org.jboss.as.jsr88.spi;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.TargetException;

/**
 * A Target interface represents a single logical core server of one instance of a J2EE platform product. It is a designator for
 * a server and the implied location to copy a configured application for the server to access.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Scott.Stark@jboss.com
 */
abstract class JBossTarget implements Target {

    /**
     * Deploy a given module
     */
    abstract void deploy(TargetModuleID targetModuleID) throws Exception;

    /**
     * Start a given module
     */
    abstract void start(TargetModuleID targetModuleID) throws Exception;

    /**
     * Stop a given module
     */
    abstract void stop(TargetModuleID targetModuleID) throws Exception;

    /**
     * Undeploy a given module
     */
    abstract void undeploy(TargetModuleID targetModuleID) throws Exception;

    /**
     * Retrieve the list of all J2EE application modules running or not running on the identified targets.
     */
    abstract TargetModuleID[] getAvailableModules(ModuleType moduleType) throws TargetException;
}
