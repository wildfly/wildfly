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

package org.jboss.as.pojo.descriptor;

import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

import java.io.Serializable;

/**
 * The module meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ModuleConfig extends AbstractConfigVisitorNode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String moduleName;
    private final InjectedValue<Module> injectedModule = new InjectedValue<Module>();

    @Override
    public void visit(ConfigVisitor visitor) {
        if (moduleName != null) {
            ModuleIdentifier identifier = ModuleIdentifier.fromString(moduleName);
            if (moduleName.startsWith(ServiceModuleLoader.MODULE_PREFIX)) {
                ServiceName serviceName = ServiceModuleLoader.moduleServiceName(identifier);
                visitor.addDependency(serviceName, getInjectedModule());
            } else {
                Module dm = visitor.loadModule(identifier);
                getInjectedModule().setValue(new ImmediateValue<Module>(dm));
            }
        } else {
            getInjectedModule().setValue(new ImmediateValue<Module>(visitor.getModule()));
        }
        // no children, no need to visit
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public InjectedValue<Module> getInjectedModule() {
        return injectedModule;
    }
}