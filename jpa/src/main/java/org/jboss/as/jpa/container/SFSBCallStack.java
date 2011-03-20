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

import org.jboss.as.jpa.spi.SFSBContextHandle;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

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
    private static ThreadLocal<ArrayList<SFSBContextHandle>> SFSBInvocationStack = new ThreadLocal<ArrayList<SFSBContextHandle>>() {
        protected synchronized ArrayList<SFSBContextHandle> initialValue() {
            return new ArrayList<SFSBContextHandle>();
        }
    };

    /**
     * For the current thread, look at the call stack of SFSB invocations and return the first extended
     * persistence context that is based on puName.
     *
     * @param puScopedName Scoped pu name
     * @return the found XPC that matches puName or null if not found
     */
    public static EntityManager findPersistenceContext(String puScopedName) {
        // TODO: arrange for a more optimal datastructure for this
        for (SFSBContextHandle handle : currentSFSBCallStack()) {
            List<EntityManager> xpcs = SFSBXPCMap.getINSTANCE().getXPC(handle);
            for (EntityManager xpc : xpcs) {
                if (xpc.unwrap(EntityManagerMetadata.class).getScopedPuName().equals(puScopedName)) {
                    return xpc;
                }
            }
        }
        return null;
    }

    /**
     * Return the current SFSB call stack
     *
     * @return call stack (may be empty but never null)
     */
    public static ArrayList<SFSBContextHandle> currentSFSBCallStack() {
        return SFSBInvocationStack.get();
    }

    /**
     * Push the passed SFSB context handle onto the invocation call stack
     *
     * @param beanContextHandle
     */
    public static void pushCall(SFSBContextHandle beanContextHandle) {
        currentSFSBCallStack().add(beanContextHandle);
    }

    /**
     * Pops the current SFSB invocation off the invocation call stack
     *
     * @return the popped SFSB context handle
     */
    public static SFSBContextHandle popCall() {
        ArrayList<SFSBContextHandle> stack = currentSFSBCallStack();
        SFSBContextHandle result = stack.remove(stack.size() - 1);
        stack.trimToSize();
        return result;
    }
}
