/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.wildfly.common.function.ThreadLocalStack;

/**
 * Close the non tx invocations on transaction scoped entity manager
 *
 * @author Scott Marlow
 */
public class NonTxEmCloser {

    /**
     * Each thread will have its own list of SB invocations in progress.
     * Key = scoped persistence unit name
     */
    public static final ThreadLocalStack<Map<String, ScopedObjects>> nonTxStack = new ThreadLocalStack<>();

    /**
     * entered new session bean invocation, start new collection for tracking transactional entity managers created
     * without a Jakarta Transactions transaction.
     */
    public static void pushCall() {
        nonTxStack.push(null);          // to conserve memory/cpu cycles, push a null placeholder that will only get replaced
        // with a Map if we actually need it (in add() below).
    }

    /**
     * current session bean invocation is ending, close any transactional entity managers created without a Jakarta Transactions
     * transaction.
     */
    public static void popCall() {
        Map<String, ScopedObjects> emStack = nonTxStack.pop();
        if (emStack != null) {
            for (ScopedObjects scopedObjects : emStack.values()) {
                try {
                    EntityManager entityManager = scopedObjects.getEntityManager();
                    if (entityManager.isOpen()) {
                        entityManager.close();
                    }
                } catch (RuntimeException safeToIgnore) {
                    if (ROOT_LOGGER.isTraceEnabled()) {
                        ROOT_LOGGER.trace("Could not close (non-transactional) container managed entity manager." +
                            "  This shouldn't impact application functionality (only read " +
                            "operations occur in non-transactional mode)", safeToIgnore);
                    }
                }
                try {
                    AutoCloseable statelessSession = scopedObjects.getStatelessSession();
                    statelessSession.close();
                } catch (Exception safeToIgnore) {
                    if (ROOT_LOGGER.isTraceEnabled()) {
                        ROOT_LOGGER.trace("Could not close (non-transactional) container managed stateless session." +
                                "  This shouldn't impact application functionality (only read " +
                                "operations occur in non-transactional mode)", safeToIgnore);
                    }
                }
            }
        }
    }

    /**
     * Return the transactional entity manager for the specified scoped persistence unit name
     *
     * @param puScopedName
     * @return
     */
    public static <T extends AutoCloseable> T get(Class<T> type, String puScopedName) {
        Map<String, ScopedObjects> map = nonTxStack.peek();
        if (map != null) {
            ScopedObjects objects = map.get(puScopedName);
            if (objects != null) {
                return objects.get(type);
            }
        }
        return null;
    }

    public static <T extends AutoCloseable> void add(String puScopedName, T scopedObject) {
        Map<String, ScopedObjects> map = nonTxStack.peek();
        if (map == null && !nonTxStack.isEmpty()) {
            // replace null with a collection to hold the entity managers.
            map = new HashMap<>();
            nonTxStack.pop();
            nonTxStack.push(map);    // replace top of stack (currently null) with new collection
        }
        if (map != null) {
            ScopedObjects objects = map.computeIfAbsent(puScopedName, k -> new ScopedObjects());
            objects.set(scopedObject);
        }
    }
}
