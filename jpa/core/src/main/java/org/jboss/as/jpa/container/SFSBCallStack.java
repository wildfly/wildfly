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
import java.util.HashMap;
import java.util.Map;

/**
 * For tracking of SFSB call stack on a per thread basis.
 * When a SFSB with an extended persistence context (XPC) is injected, the SFSB call stack is searched for
 * a XPC that can be inherited from.
 *
 * @author Scott Marlow
 */
public class SFSBCallStack {

    /**
     * Each thread will have its own list of SFSB invocations in progress.
     */
    private static ThreadLocal<ArrayList<Map<String, ReferenceCountedEntityManager>>> SFSBInvocationStack = new ThreadLocal<ArrayList<Map<String, ReferenceCountedEntityManager>>>() {
        protected ArrayList<Map<String, ReferenceCountedEntityManager>> initialValue() {
            return new ArrayList<Map<String, ReferenceCountedEntityManager>>();
        }
    };

    /**
     * Entity managers that form part of the
     */
    private static ThreadLocal<Map<String, ReferenceCountedEntityManager>> sfsbCreationMap = new ThreadLocal<Map<String, ReferenceCountedEntityManager>>();

    private static ThreadLocal<Integer> sfsbCreationCallStackCount = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public static void beginSfsbCreation() {
        int no = sfsbCreationCallStackCount.get();
        if (no == 0) {
            sfsbCreationMap.set(new HashMap<String, ReferenceCountedEntityManager>());
        }
        sfsbCreationCallStackCount.set(no + 1);
    }

    public static void endSfsbCreation() {
        int no = sfsbCreationCallStackCount.get();
        no--;
        sfsbCreationCallStackCount.set(no);
        if (no == 0) {
            sfsbCreationMap.remove();
        }
    }

    public static void extendedPersistenceContextCreated(String scopedPuName, ReferenceCountedEntityManager entityManager) {
        if (sfsbCreationCallStackCount.get() > 0) {
            Map<String, ReferenceCountedEntityManager> map = sfsbCreationMap.get();
            if (!map.containsKey(scopedPuName)) {
                map.put(scopedPuName, entityManager);
            }
        }
    }

    /**
     * For the current thread, look at the call stack of SFSB invocations and return the first extended
     * persistence context that is based on puName.
     *
     *
     * @param puScopedName Scoped pu name
     * @return the found XPC that matches puName or null if not found
     */
    public static ReferenceCountedEntityManager findPersistenceContext(String puScopedName) {
        // TODO: arrange for a more optimal datastructure for this
        for (Map<String, ReferenceCountedEntityManager> handle : currentSFSBCallStack()) {
            final ReferenceCountedEntityManager res = handle.get(puScopedName);
            if(res != null) {
                return res;
            }
        }
        Map<String, ReferenceCountedEntityManager> map = sfsbCreationMap.get();
        if (map != null) {
            return map.get(puScopedName);
        }
        return null;
    }

    /**
     * Return the current SFSB call stack
     *
     * @return call stack (may be empty but never null)
     */
    public static ArrayList<Map<String, ReferenceCountedEntityManager>> currentSFSBCallStack() {
        return SFSBInvocationStack.get();
    }

    /**
     * Push the passed SFSB context handle onto the invocation call stack
     *
     * @param entityManagers the entity manager map
     */
    public static void pushCall(Map<String, ReferenceCountedEntityManager> entityManagers) {
        currentSFSBCallStack().add(entityManagers);
    }

    /**
     * Pops the current SFSB invocation off the invocation call stack
     *
     * @return the popped SFSB context handle
     */
    public static Map<String, ReferenceCountedEntityManager> popCall() {
        ArrayList<Map<String, ReferenceCountedEntityManager>> stack = currentSFSBCallStack();
        Map<String, ReferenceCountedEntityManager> result = stack.remove(stack.size() - 1);
        stack.trimToSize();
        return result;
    }
}
