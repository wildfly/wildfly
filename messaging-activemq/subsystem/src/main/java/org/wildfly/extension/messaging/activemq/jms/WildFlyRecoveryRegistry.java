/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;


import org.jboss.activemq.artemis.wildfly.integration.recovery.WildFlyActiveMQRegistry;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         9/22/11
 */
public class WildFlyRecoveryRegistry extends WildFlyActiveMQRegistry {
    static volatile ServiceContainer container;

    private XAResourceRecoveryRegistry registry;

    public WildFlyRecoveryRegistry() {
       registry = getXAResourceRecoveryRegistry();
       if (registry == null) {
           throw MessagingLogger.ROOT_LOGGER.unableToFindRecoveryRegistry();
       }
    }

    public XAResourceRecoveryRegistry getTMRegistry() {
       return registry;
    }

    private static XAResourceRecoveryRegistry getXAResourceRecoveryRegistry() {
        // This parsing isn't 100% ideal as it's somewhat 'internal' knowledge of the relationship between
        // capability names and service names. But at this point that relationship really needs to become
        // a contract anyway
        ServiceName serviceName = ServiceNameFactory.parseServiceName(MessagingServices.TRANSACTION_XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY);
        @SuppressWarnings("unchecked")
        ServiceController<XAResourceRecoveryRegistry> service = (ServiceController<XAResourceRecoveryRegistry>) container.getService(serviceName);
        return service == null ? null : service.getValue();
    }
}
