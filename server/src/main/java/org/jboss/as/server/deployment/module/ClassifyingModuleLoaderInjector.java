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

package org.jboss.as.server.deployment.module;

import org.jboss.modules.ModuleLoader;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

/**
 * Injector used to inject a ModuleLoader into a Classifying module loader with a name.
 *
 * @author John Bailey
 */
public class ClassifyingModuleLoaderInjector implements Injector<ClassifyingModuleLoaderService> {
    private final String name;
    private Value<? extends ModuleLoader> moduleLoaderValue;
    private ClassifyingModuleLoaderService classifyingModuleLoaderService;

    /**
     * Create an instance.
     *
     * @param name The module loader name
     * @param moduleLoaderValue The module loader
     */
    public ClassifyingModuleLoaderInjector(String name, Value<? extends ModuleLoader> moduleLoaderValue) {
        this.name = name;
        this.moduleLoaderValue = moduleLoaderValue;
    }

    /**
     * Add the module loader to the classifying module loader with provided name.
     *
     * @param classifyingModuleLoaderService The classifying loader to inject into.
     * @throws InjectionException
     */
    public void inject(final ClassifyingModuleLoaderService classifyingModuleLoaderService) throws InjectionException {
        this.classifyingModuleLoaderService = classifyingModuleLoaderService;
        classifyingModuleLoaderService.addDelegate(name, moduleLoaderValue.getValue());
    }

    /**
     * Remove the module loader from the classifying module loader.
     */
    public void uninject() {
        classifyingModuleLoaderService.removeDelegate(name);
    }
}
