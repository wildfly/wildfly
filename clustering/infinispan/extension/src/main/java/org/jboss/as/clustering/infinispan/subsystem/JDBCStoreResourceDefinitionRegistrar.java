/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.infinispan.persistence.jdbc.JDBCStoreConfiguration;
import org.jboss.as.clustering.infinispan.persistence.jdbc.JDBCStoreConfigurationBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;

/**
 * Base class for store resources which require common store attributes and JDBC store attributes
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JDBCStoreResourceDefinitionRegistrar extends StoreResourceDefinitionRegistrar<JDBCStoreConfiguration, JDBCStoreConfigurationBuilder> {

    JDBCStoreResourceDefinitionRegistrar() {
        super(JDBCStoreResourceDescription.INSTANCE);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);
        new ComponentResourceDefinitionRegistrar<>(TableResourceDescription.INSTANCE).register(registration, context);
        return registration;
    }
}
