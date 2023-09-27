/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.container;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.jpa.messages.JpaLogger;

/**
 * Tracks the lifecycle of created XPC Entity Managers
 *
 * @author Scott Marlow
 */
public class CreatedEntityManagers {

    private static ExtendedEntityManager[] EMPTY = new ExtendedEntityManager[0];

    // at injection time, the SFSB that is being created isn't registered right away
    // that happens later at postConstruct time.
    //
    // The deferToPostConstruct is a one item length store (hack)
    private static ThreadLocal<List<ExtendedEntityManager>> deferToPostConstruct = new ThreadLocal<List<ExtendedEntityManager>>() {
        protected List<ExtendedEntityManager> initialValue() {
            return new ArrayList<ExtendedEntityManager>(1);
        }
    };

    /**
     * At injection time of a XPC, register the XPC (step 1 of 2)
     * finishRegistrationOfPersistenceContext is step 2
     *
     * @param xpc The ExtendedEntityManager
     */
    public static void registerPersistenceContext(ExtendedEntityManager xpc) {
        if (xpc == null) {
            throw JpaLogger.ROOT_LOGGER.nullParameter("SFSBXPCMap.RegisterPersistenceContext", "EntityManager");
        }
        final List<ExtendedEntityManager> store = deferToPostConstruct.get();
        store.add(xpc);
    }

    /**
     * Called by postconstruct interceptor
     */
    public static ExtendedEntityManager[] getDeferredEntityManagers() {
        List<ExtendedEntityManager> store = deferToPostConstruct.get();
        try {
            if(store.isEmpty()) {
                return EMPTY;
            } else {
                return store.toArray(new ExtendedEntityManager[store.size()]);
            }
        } finally {
            store.clear();
        }
    }

}
