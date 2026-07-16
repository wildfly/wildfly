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
    private static volatile Supplier<XAResourceRecoveryRegistry> supplier;

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

    public static void setSupplier(Supplier<XAResourceRecoveryRegistry> s) {
        if (supplier != null) {
            MessagingLogger.ROOT_LOGGER.recoveryRegistrySupplierAlreadySet();
            return;
        }
        supplier = s;
    }

    public static void clearSupplier() {
        supplier = null;
    }

    private static XAResourceRecoveryRegistry getXAResourceRecoveryRegistry() {
        return supplier == null ? null : supplier.get();
    }
}
