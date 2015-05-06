/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
