/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.SessionManagementResourceDefinition.Attribute.GRANULARITY;
import static org.wildfly.extension.clustering.web.SessionManagementResourceDefinition.Attribute.MARSHALLER;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.web.service.WebProviderRequirement;
import org.wildfly.clustering.web.service.routing.RouteLocatorServiceConfiguratorFactory;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.session.SessionAttributePersistenceStrategy;

/**
 * Abstract service configurator for session management providers.
 * @author Paul Ferraro
 */
public abstract class SessionManagementServiceConfigurator<C extends DistributableSessionManagementConfiguration<DeploymentUnit>> extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, DistributableSessionManagementConfiguration<DeploymentUnit>, Supplier<DistributableSessionManagementProvider<C>> {

    private volatile SessionGranularity granularity;
    private volatile SessionMarshallerFactory marshallerFactory;
    private volatile SupplierDependency<RouteLocatorServiceConfiguratorFactory<C>> factory;

    SessionManagementServiceConfigurator(PathAddress address) {
        super(SessionManagementResourceDefinition.Capability.SESSION_MANAGEMENT_PROVIDER, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.granularity = SessionGranularity.valueOf(GRANULARITY.resolveModelAttribute(context, model).asString());
        this.marshallerFactory = SessionMarshallerFactory.valueOf(MARSHALLER.resolveModelAttribute(context, model).asString());
        this.factory = new ServiceSupplierDependency<>(WebProviderRequirement.AFFINITY.getServiceName(context, this.getServiceName().getSimpleName()));
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<DistributableSessionManagementProvider<C>> provider = this.factory.register(builder).provides(name);
        Service service = new FunctionalService<>(provider, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return this.granularity.getAttributePersistenceStrategy();
    }

    @Override
    public Function<DeploymentUnit, ByteBufferMarshaller> getMarshallerFactory() {
        return this.marshallerFactory;
    }

    public RouteLocatorServiceConfiguratorFactory<C> getRouteLocatorServiceConfiguratorFactory() {
        return this.factory.get();
    }
}
