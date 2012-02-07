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

import static org.jboss.as.jpa.JpaMessages.MESSAGES;

/**
 * Tracks the lifecycle of created XPC Entity Managers
 *
 * @author Scott Marlow
 */
public class CreatedEntityManagers {

    // at injection time, the SFSB that is being created isn't registered right away
    // that happens later at postConstruct time.
    //
    // The deferToPostConstruct is a one item length store (hack)
    private static ThreadLocal<List<ReferenceCountedEntityManager>> deferToPostConstruct = new ThreadLocal<List<ReferenceCountedEntityManager>>() {
        protected List<ReferenceCountedEntityManager> initialValue() {
            return new ArrayList<ReferenceCountedEntityManager>(1);
        }
    };

    /**
     * At injection time of a XPC, register the XPC (step 1 of 2)
     * finishRegistrationOfPersistenceContext is step 2
     *
     * @param xpc The ExtendedEntityManager
     */
    public static void registerPersistenceContext(ReferenceCountedEntityManager xpc) {
        if (xpc == null) {
            throw MESSAGES.nullParameter("SFSBXPCMap.RegisterPersistenceContext", "EntityManager");
        }
        final List<ReferenceCountedEntityManager> store = deferToPostConstruct.get();
        store.add(xpc);
    }

    /**
     * Called by postconstruct interceptor
     */
    public static List<ReferenceCountedEntityManager> getDeferredEntityManagers() {
        List<ReferenceCountedEntityManager> store = deferToPostConstruct.get();
        try {
            return new ArrayList<ReferenceCountedEntityManager>(store);
        } finally {
            store.clear();
        }
    }

}
