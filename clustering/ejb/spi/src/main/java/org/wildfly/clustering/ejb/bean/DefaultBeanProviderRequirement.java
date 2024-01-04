/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.bean;

import org.jboss.as.clustering.controller.RequirementServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactory;
import org.jboss.as.clustering.controller.ServiceNameFactoryProvider;
import org.wildfly.clustering.service.Requirement;

/**
 * Requirements representing services names for bean management providers
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public enum DefaultBeanProviderRequirement implements Requirement, ServiceNameFactoryProvider {
    BEAN_MANAGEMENT_PROVIDER("org.wildfly.clustering.ejb.default-bean-management-provider", BeanManagementProvider.class),
    ;
    private final String name;
    private final Class<?> type;
    private final ServiceNameFactory factory;

    DefaultBeanProviderRequirement(String name, Class<?> type) {
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
