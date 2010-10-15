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

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleSpec;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default deployment module loader.  Maintains a map of module specs that can be loaded at a later time.
 *
 * @author John E. Bailey
 */
public class DeploymentModuleLoaderImpl extends DeploymentModuleLoader {
    private final ConcurrentMap<ModuleIdentifier, ModuleSpec> moduleSpecs = new ConcurrentHashMap<ModuleIdentifier, ModuleSpec>();

    public DeploymentModuleLoaderImpl() {
    }

    @Override
    public void addModuleSpec(ModuleSpec moduleSpec) {
        if(moduleSpecs.putIfAbsent(moduleSpec.getModuleIdentifier(), moduleSpec) != null) {
            throw new IllegalArgumentException("Module spec has already been added for identifier [" + moduleSpec.getModuleIdentifier() + "]");
        }
    }

    @Override
    protected Module preloadModule(final ModuleIdentifier identifier) throws ModuleLoadException {
        if (identifier.getName().startsWith("deployment.")) {
            return super.preloadModule(identifier);
        } else {
            return Module.getModuleFromDefaultLoader(identifier);
        }
    }

    @Override
    protected ModuleSpec findModule(ModuleIdentifier moduleIdentifier) throws ModuleLoadException {
        final ConcurrentMap<ModuleIdentifier, ModuleSpec> moduleSpecs = this.moduleSpecs;
        ModuleSpec moduleSpec = moduleSpecs.get(moduleIdentifier);
        return moduleSpec;
    }

    @Override
    public void removeModule(Module module) {
        if (moduleSpecs.remove(module.getIdentifier()) != null) {
            unloadModuleLocal(module);
        }
        else throw new IllegalStateException("Unknown module " + module);
    }

    public String toString() {
        return "as-deployment";
    }
}
