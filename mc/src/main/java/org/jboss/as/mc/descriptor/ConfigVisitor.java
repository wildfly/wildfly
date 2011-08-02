/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mc.descriptor;

import org.jboss.as.mc.BeanState;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;

/**
 * Config visitor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface ConfigVisitor {
    /**
     * Get current state.
     *
     * @return the state
     */
    BeanState getState();

    /**
     * Get module for this visitor.
     *
     * @return the classloader
     */
    Module getModule();

    /**
     * Load module.
     *
     * @param identifier the module identifier
     * @return loaded module
     */
    Module loadModule(ModuleIdentifier identifier);

    /**
     * Add dependency.
     *
     * @param name the dependency name
     */
    void addDependency(ServiceName name);

    /**
     * Add dependency.
     *
     * @param name the dependency name
     * @param injector the injector
     */
    void addDependency(ServiceName name, Injector injector);
}
