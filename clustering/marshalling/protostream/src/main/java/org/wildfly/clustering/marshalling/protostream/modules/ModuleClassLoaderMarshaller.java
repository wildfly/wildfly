/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.modules;

import java.io.IOException;
import java.io.InvalidClassException;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.marshalling.protostream.ClassLoaderMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class ModuleClassLoaderMarshaller implements ClassLoaderMarshaller {

    private static final int MODULE_INDEX = 0;
    private static final int FIELDS = 1;

    private final ModuleLoader loader;
    private final Module defaultModule;

    public ModuleClassLoaderMarshaller(Module defaultModule) {
        this.loader = defaultModule.getModuleLoader();
        this.defaultModule = defaultModule;
    }

    public ModuleClassLoaderMarshaller(ModuleLoader loader) {
        this.loader = loader;
        try {
            this.defaultModule = Module.getSystemModuleLoader().loadModule("java.base");
        } catch (ModuleLoadException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ClassLoader createInitialValue() {
        return this.defaultModule.getClassLoader();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public ClassLoader readFrom(ProtoStreamReader reader, int index, WireType type, ClassLoader loader) throws IOException {
        switch (index) {
            case MODULE_INDEX:
                String moduleName = reader.readAny(String.class);
                try {
                    Module module = this.loader.loadModule(moduleName);
                    return module.getClassLoader();
                } catch (ModuleLoadException e) {
                    InvalidClassException exception = new InvalidClassException(e.getMessage());
                    exception.initCause(e);
                    throw exception;
                }
            default:
                reader.skipField(type);
                return loader;
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ClassLoader loader) throws IOException {
        Module module = Module.forClassLoader(loader, false);
        if (module != null && !this.defaultModule.equals(module)) {
            writer.writeAny(MODULE_INDEX, module.getName());
        }
    }
}
