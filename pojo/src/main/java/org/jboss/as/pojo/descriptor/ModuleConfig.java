/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;
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
            if (moduleName.startsWith(ServiceModuleLoader.MODULE_PREFIX)) {
                ServiceName serviceName = ServiceModuleLoader.moduleServiceName(moduleName);
                visitor.addDependency(serviceName, getInjectedModule());
            } else {
                Module dm = visitor.loadModule(moduleName);
                getInjectedModule().setValue(() -> dm);
            }
        } else {
            getInjectedModule().setValue(visitor::getModule);
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