/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;


import java.util.function.Supplier;
import org.jboss.activemq.artemis.wildfly.integration.recovery.WildFlyActiveMQRegistry;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         9/22/11
 */
public class WildFlyRecoveryRegistry extends WildFlyActiveMQRegistry {
    static volatile Supplier<XAResourceRecoveryRegistry> supplier;

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
        return supplier == null ? null : supplier.get();
    }
}
