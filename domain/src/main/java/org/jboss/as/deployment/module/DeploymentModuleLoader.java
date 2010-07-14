/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment.module;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;

/**
 * Module loader capable of having modules added and removed at runtime.  The primary goal is to support deployment units being
 * built into modules.  The basic contract requires the module spec to be added to the loader and the module can be
 * loaded at a later time using the normal ModuleLoader.loadModule method.
 *
 * @author John E. Bailey
 */
public abstract class DeploymentModuleLoader extends ModuleLoader {
    /**
     * Add a module spec to the module loader enabling it to be looked up at a later time.
     *
     * @param moduleSpec The module spec to add
     */
    public abstract void addModuleSpec(ModuleSpec moduleSpec);

    /**
     * Remove a module from the module loader.
     *
     * @param moduleIdentifier The module identifier
     */
    public abstract void removeModule(ModuleIdentifier moduleIdentifier);
}
