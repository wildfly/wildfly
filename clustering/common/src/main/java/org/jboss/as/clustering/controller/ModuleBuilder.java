/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class ModuleBuilder implements ResourceServiceBuilder<Module>, Service<Module> {

    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final ServiceName name;
    private final Attribute attribute;

    private volatile String identifier;
    private volatile Module module;

    public ModuleBuilder(ServiceName name, Attribute attribute) {
        this.name = name;
        this.attribute = attribute;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public ServiceBuilder<Module> build(ServiceTarget target) {
        return target.addService(this.name, this)
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                ;
    }

    @Override
    public Builder<Module> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.identifier = this.attribute.resolveModelAttribute(context, model).asString();
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.module = this.loader.getValue().loadModule(this.identifier);
        } catch (ModuleLoadException e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        this.module = null;
    }

    @Override
    public Module getValue() {
        return this.module;
    }
}
