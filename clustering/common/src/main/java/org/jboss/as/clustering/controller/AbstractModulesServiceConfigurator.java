/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SimpleServiceNameProvider;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractModulesServiceConfigurator<T> extends SimpleServiceNameProvider implements ResourceServiceConfigurator, Supplier<List<Module>>, Function<List<Module>, T> {

    private final Attribute attribute;
    private final SupplierDependency<ModuleLoader> loader = new ServiceSupplierDependency<>(Services.JBOSS_SERVICE_MODULE_LOADER);
    private final Function<ModelNode, List<ModelNode>> toList;

    private volatile List<ModelNode> identifiers = Collections.emptyList();

    AbstractModulesServiceConfigurator(ServiceName name, Attribute attribute, Function<ModelNode, List<ModelNode>> toList) {
        super(name);
        this.attribute = attribute;
        this.toList = toList;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.identifiers = this.toList.apply(this.attribute.resolveModelAttribute(context, model));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<T> modules = this.loader.register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(modules, this, this);
        return builder.setInstance(service);
    }

    @Override
    public List<Module> get() {
        List<ModelNode> identifiers = this.identifiers;
        List<Module> modules = !identifiers.isEmpty() ? new ArrayList<>(identifiers.size()) : Collections.emptyList();
        for (ModelNode identifier : identifiers) {
            try {
                modules.add(this.loader.get().loadModule(identifier.asString()));
            } catch (ModuleLoadException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return modules;
    }
}
