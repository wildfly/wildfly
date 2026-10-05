/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;


import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jboss.activemq.artemis.wildfly.integration.recovery.WildFlyActiveMQRegistry;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         9/22/11
 */
public class WildFlyRecoveryRegistry extends WildFlyActiveMQRegistry {

    /**
     * Suppliers of the {@link XAResourceRecoveryRegistry}, each contributed by a messaging service
     * (pooled-connection-factory, external pooled-connection-factory or JMS bridge) that requires XA
     * recovery. A supplier obtained from {@code ServiceBuilder.requires(...)} only returns a non-null
     * value while its owning service is up, so we keep every registrant and resolve against the first
     * live one. The capability resolves to a single global {@link XAResourceRecoveryRegistry}, therefore
     * any non-null supplier returns the same registry instance. Keeping a collection (rather than a
     * single supplier) prevents recovery from being pinned to one service that may be down after a
     * connection failure and restart.
     */
    private static final Set<Supplier<XAResourceRecoveryRegistry>> SUPPLIERS = ConcurrentHashMap.newKeySet();

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

    /**
     * Registers a supplier of the recovery registry contributed by a messaging service. Safe to call
     * repeatedly; duplicate registrations of the same supplier are ignored by the underlying set.
     */
    public static void registerSupplier(Supplier<XAResourceRecoveryRegistry> supplier) {
        if (supplier != null) {
            SUPPLIERS.add(supplier);
        }
    }

    /**
     * Deregisters a supplier previously registered via {@link #registerSupplier(Supplier)}. Messaging
     * services call this from their {@code stop()} so a stopped service no longer contributes.
     */
    public static void deregisterSupplier(Supplier<XAResourceRecoveryRegistry> supplier) {
        if (supplier != null) {
            SUPPLIERS.remove(supplier);
        }
    }

    /**
     * Removes all registered suppliers. Intended for tests.
     */
    public static void clearSuppliers() {
        SUPPLIERS.clear();
    }

    private static XAResourceRecoveryRegistry getXAResourceRecoveryRegistry() {
        for (Supplier<XAResourceRecoveryRegistry> supplier : SUPPLIERS) {
            XAResourceRecoveryRegistry candidate = supplier.get();
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }
}
