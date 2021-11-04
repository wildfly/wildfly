package org.wildfly.clustering.ejb;

import org.jboss.as.clustering.controller.RequirementServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactoryProvider;
import org.wildfly.clustering.service.Requirement;

public enum EjbRequirement implements Requirement, ServiceNameFactoryProvider {
    CLIENT_MAPPINGS_REGISTRY_PROVIDER("org.wildfly.clusering.ejb.client-mappings-registry-provider", ClientMappingsRegistryProvider.class)
    ;
    private final String name;
    private final Class<?> type;
    private final ServiceNameFactory factory;

    EjbRequirement(final String name, final Class<?> type) {
        this.name = name;
        this.type = type;
        this.factory = new RequirementServiceNameFactory(this);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public ServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
