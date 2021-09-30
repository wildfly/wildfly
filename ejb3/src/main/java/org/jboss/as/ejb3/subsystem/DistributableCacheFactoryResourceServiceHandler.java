package org.jboss.as.ejb3.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderServiceNameProvider;
import org.jboss.as.ejb3.cache.distributable.DistributableCacheFactoryBuilderServiceConfigurator;
import org.jboss.as.ejb3.cache.distributable.DistributableCacheFactoryBuilderServiceNameProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceConfigurator;

public class DistributableCacheFactoryResourceServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        ServiceTarget target = context.getServiceTarget();

        // set up the CacheFactoryBuilder service
        new DistributableCacheFactoryBuilderServiceConfigurator<>(name).configure(context, model).build(target).install();

        // set up a service alias jboss.ejb.cache.factory.<name> -> jboss.ejb.cache.factory.distributable.<name>
        ServiceConfigurator configurator = new IdentityServiceConfigurator<>(
                new CacheFactoryBuilderServiceNameProvider(name).getServiceName(),
                new DistributableCacheFactoryBuilderServiceNameProvider(name).getServiceName()
        );
        ServiceBuilder<?> builder = configurator.build(target);
        builder.install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();

        // remove the CacheFactoryBuilder service for a distributable, passivating cache factory
        context.removeService(new DistributableCacheFactoryBuilderServiceConfigurator<>(name).getServiceName());

        // remove service alias
        context.removeService(new CacheFactoryBuilderServiceNameProvider(name).getServiceName());
    }
}
