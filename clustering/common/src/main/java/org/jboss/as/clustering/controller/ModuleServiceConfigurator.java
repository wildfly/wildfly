/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Configures a service providing a {@link Module}.
 * @author Paul Ferraro
 */
public class ModuleServiceConfigurator extends SimpleServiceNameProvider implements ResourceServiceConfigurator, Supplier<Module> {

    private final Attribute attribute;

    private volatile String identifier;
    private volatile Supplier<ModuleLoader> loader;

    public ModuleServiceConfigurator(ServiceName name, Attribute attribute) {
        super(name);
        this.attribute = attribute;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.identifier = this.attribute.resolveModelAttribute(context, model).asString();
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<Module> module = builder.provides(this.getServiceName());
        this.loader = builder.requires(Services.JBOSS_SERVICE_MODULE_LOADER);
        Service service = new FunctionalService<>(module, Function.identity(), this);
        return builder.setInstance(service);
    }

    @Override
    public Module get() {
        try {
            return this.loader.get().loadModule(this.identifier);
        } catch (ModuleLoadException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
