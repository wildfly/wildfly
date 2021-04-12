/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InvalidClassException;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

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
    public ClassLoader getBuilder() {
        return this.defaultModule.getClassLoader();
    }

    @Override
    public int getFields() {
        return FIELDS;
    }

    @Override
    public ClassLoader readField(ProtoStreamReader reader, int index, ClassLoader loader) throws IOException {
        switch (index) {
            case MODULE_INDEX:
                String moduleName = reader.readString();
                try {
                    Module module = this.loader.loadModule(moduleName);
                    return module.getClassLoader();
                } catch (ModuleLoadException e) {
                    InvalidClassException exception = new InvalidClassException(e.getMessage());
                    exception.initCause(e);
                    throw exception;
                }
            default:
                return loader;
        }
    }

    @Override
    public void writeFields(ProtoStreamWriter writer, int startIndex, ClassLoader loader) throws IOException {
        Module module = Module.forClassLoader(loader, false);
        if (module != null && !this.defaultModule.equals(module)) {
            writer.writeString(startIndex + MODULE_INDEX, module.getName());
        }
    }
}
