/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector._private;

import javax.sql.DataSource;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.naming.service.NamingService;

/**
 * Capabilities for the connector subsystems.
 * <p>
 * <strong>This is not to be used outside of the various connector subsystems.</strong>
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface Capabilities {
    /**
     * The name for the data-source capability
     */
    String DATA_SOURCE_CAPABILITY_NAME = "org.wildfly.data-source";

    /**
     * The name of the authentication-context capability provided by Elytron.
     */
    String AUTHENTICATION_CONTEXT_CAPABILITY = "org.wildfly.security.authentication-context";

    String ELYTRON_SECURITY_DOMAIN_CAPABILITY = "org.wildfly.security.security-domain";

    String RESOURCE_ADAPTER_CAPABILITY_NAME = "org.wildfly.resource-adapter";

    String JCA_NAMING_CAPABILITY_NAME = "org.wildfly.jca.naming";

    /**
     * The data-source capability
     */
    RuntimeCapability<Void> DATA_SOURCE_CAPABILITY = RuntimeCapability.Builder.of(DATA_SOURCE_CAPABILITY_NAME, true, DataSource.class)
            .addRequirements(NamingService.CAPABILITY_NAME, ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME)
            .build();

    RuntimeCapability<Void> RESOURCE_ADAPTER_CAPABILITY = RuntimeCapability.Builder.of(RESOURCE_ADAPTER_CAPABILITY_NAME, true)
            .addRequirements(ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME, NamingService.CAPABILITY_NAME)
            .build();

    RuntimeCapability<Void> JCA_NAMING_CAPABILITY = RuntimeCapability.Builder.of(JCA_NAMING_CAPABILITY_NAME)
            .addRequirements(NamingService.CAPABILITY_NAME)
            .build();
}
