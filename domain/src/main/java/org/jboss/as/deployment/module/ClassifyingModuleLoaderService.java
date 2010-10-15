/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.util.HashMap;
import java.util.Map;
import org.jboss.modules.ClassifyingModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleLoaderSelector;
import org.jboss.modules.SimpleModuleLoaderSelector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for managing the lifecycle of the classifying module loader.
 *
 * @author John Bailey
 */
public class ClassifyingModuleLoaderService implements Service<ClassifyingModuleLoaderService> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("classifying", "module", "loader");
    private ClassifyingModuleLoader classifyingModuleLoader;
    private final Map<String, ModuleLoader> delegates = new HashMap<String, ModuleLoader>();

    /**
     * Creates the classifying module loader with the current set of delegates, and registers a module
     * loader selector.
     *
     * @param context The start context
     */
    public synchronized void start(StartContext context) throws StartException {
        classifyingModuleLoader = new ClassifyingModuleLoader("as-classifying", delegates, Module.getDefaultModuleLoader());
    }

    /**
     * Removes the reference to the classifying module loader and remove the selector.
     *
     * @param context
     */
    public synchronized void stop(StopContext context) {
        classifyingModuleLoader = null;
    }

    /** {@inheritDoc} */
    public synchronized ClassifyingModuleLoaderService getValue() throws IllegalStateException {
        return this;
    }

    synchronized void addDelegate(final String name, final ModuleLoader delegate) {
        if(delegates.put(name, delegate) != null) {
            throw new IllegalArgumentException("Provided delegate name " + name + " is already present.");
        }
        classifyingModuleLoader.setDelegates(delegates);
    }

    synchronized void removeDelegate(final String name) {
        delegates.remove(name);
        classifyingModuleLoader.setDelegates(delegates);
    }
}
