/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.modules;

import java.util.Optional;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderResolver;

/**
 * @author Paul Ferraro
 */
public class ModuleClassResolver implements ClassLoaderResolver {

    private final ModuleLoader loader;
    private final Module defaultModule;

    public ModuleClassResolver(Module defaultModule) {
        this.loader = defaultModule.getModuleLoader();
        this.defaultModule = defaultModule;
    }

    public ModuleClassResolver(ModuleLoader loader) {
        this.loader = loader;
        this.defaultModule = null;
    }

    @Override
    public String classLoaderName(Class<?> value) {
        return Optional.ofNullable(Module.forClass(value)).map(Module::getName).orElse(null);
    }

    @Override
    public ClassLoader resolve(String name) {
        try {
            Module module = this.loader.loadModule(name);
            return module.getClassLoader();
        } catch (ModuleLoadException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public ClassLoader getDefaultClassLoader() {
        return Optional.ofNullable(this.defaultModule).<ClassLoader>map(Module::getClassLoader).orElse(ClassLoader.getSystemClassLoader());
    }
}
