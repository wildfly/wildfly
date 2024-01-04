/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.util;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.msc.service.ServiceName;

/**
 * For Jakarta Persistence service names
 *
 * @author Scott Marlow
 */
public class JPAServiceNames {

    // identifies the (instance per) persistence unit service name
    private static final ServiceName PERSISTENCE_UNIT_SERVICE_NAME = ServiceName.JBOSS.append("persistenceunit");

    // identifies the (singleton) Jakarta Persistence service
    public static final ServiceName JPA_SERVICE_NAME = ServiceName.JBOSS.append("jpa");

    public static ServiceName getPUServiceName(String scopedPersistenceUnitName) {
        return PERSISTENCE_UNIT_SERVICE_NAME.append(scopedPersistenceUnitName);
    }

    public static ServiceName getJPAServiceName() {
        return JPA_SERVICE_NAME;
    }

    /**
     * Name of the capability that ensures a local provider of transactions is present.
     * Once its service is started, calls to the getInstance() methods of ContextTransactionManager,
     * and LocalUserTransaction can be made knowing that the global default TM and UT will be from that provider.
     */
    public static final String LOCAL_TRANSACTION_PROVIDER_CAPABILITY = "org.wildfly.transactions.global-default-local-provider";

    /**
     * Name of the capability that ensures a local provider of transactions is present.
     * Once its service is started, calls to the getInstance() methods of ContextTransactionManager,
     * and LocalUserTransaction can be made knowing that the global default TM and UT will be from that provider.
     */
    public static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY = "org.wildfly.transactions.transaction-synchronization-registry";

    // This parsing isn't 100% ideal as it's somewhat 'internal' knowledge of the relationship between
    // capability names and service names. But at this point that relationship really needs to become
    // a contract anyway
    public static final ServiceName TRANSACTION_SYNCHRONIZATION_REGISTRY_SERVICE = ServiceNameFactory.parseServiceName(TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY);
}
