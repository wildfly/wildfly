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

import org.jboss.as.jpa.messages.JpaLogger;

/**
 * For tracking of SFSB call stack on a per thread basis.
 * When a SFSB with an extended persistence context (XPC) is injected, the SFSB call stack is searched for
 * a XPC that can be inherited from.
 *
 * @author Scott Marlow
 */
public class SFSBCallStack {

    private static final ThreadLocal<SFSBCallStackThreadData> CURRENT = new ThreadLocal<SFSBCallStackThreadData>() {
        @Override
        protected SFSBCallStackThreadData initialValue() {
            return new SFSBCallStackThreadData();
        }
    };

    public static int getSFSBCreationBeanNestingLevel() {
        return CURRENT.get().creationBeanNestingLevel;
    }

    /**
     * called from SFSBPreCreateInterceptor, before bean creation
     */
    public static void beginSfsbCreation() {
        SFSBCallStackThreadData data = CURRENT.get();
        int no = data.creationBeanNestingLevel;
        if (no == 0) {
            data.creationTimeXPCRegistration = new HashMap<String, ExtendedEntityManager>();
            // create new tracking structure (passing in parent levels tracking structure or null if toplevel)
            data.creationTimeInjectedXPCs = new SFSBInjectedXPCs(data.creationTimeInjectedXPCs, null);
        }
        else {
            // create new tracking structure (passing in parent levels tracking structure or null if toplevel)
            SFSBInjectedXPCs parent = data.creationTimeInjectedXPCs;
            data.creationTimeInjectedXPCs = new SFSBInjectedXPCs(parent, parent.getTopLevel());
        }
        data.creationBeanNestingLevel++;
    }

    /**
     * called from SFSBPreCreateInterceptor, after bean creation
     */
    public static void endSfsbCreation() {
        SFSBCallStackThreadData data = CURRENT.get();
        int no =  data.creationBeanNestingLevel;
        no--;
        data.creationBeanNestingLevel = no;

        if (no == 0) {
            // Completed creating top level bean, remove 'xpc creation tracking' thread local
            data.creationTimeXPCRegistration = null;
            data.creationTimeInjectedXPCs = null;
        }
        else {
            // finished creating a sub-bean, switch to parent level 'xpc creation tracking'
            data.creationTimeInjectedXPCs = data.creationTimeInjectedXPCs.getParent();
        }
    }

    static SFSBInjectedXPCs getSFSBCreationTimeInjectedXPCs(final String puScopedName) {
        SFSBInjectedXPCs result = CURRENT.get().creationTimeInjectedXPCs;
        if (result == null) {
            throw JpaLogger.ROOT_LOGGER.xpcOnlyFromSFSB(puScopedName);
        }
        return result;
    }


    /**
     * Return the current entity manager call stack
     *
     * @return call stack (may be empty but never null)
     */
    public static ArrayList<Map<String, ExtendedEntityManager>> currentSFSBCallStack() {
        return CURRENT.get().invocationStack;
    }

    /**
     * return for just the current entity manager invocation
     *
     * @return
     */
    public static Map<String, ExtendedEntityManager> currentSFSBCallStackInvocation() {
        ArrayList<Map<String, ExtendedEntityManager>> stack = CURRENT.get().invocationStack;
        if ( stack != null && stack.size() > 0) {
            return stack.get(stack.size() - 1);
        }
        return null;
    }

    /**
     * Push the passed SFSB context handle onto the invocation call stack
     *
     * @param entityManagers the entity manager map
     */
    public static void pushCall(Map<String, ExtendedEntityManager> entityManagers) {
        currentSFSBCallStack().add(entityManagers);

        if (entityManagers != null) {
            /**
             * JPA 2.0 spec section 7.9.1 Container Responsibilities:
             * "When a business method of the stateful session bean is invoked,
             *  if the stateful session bean uses container managed transaction demarcation,
             *  and the entity manager is not already associated with the current JTA transaction,
             *  the container associates the entity manager with the current JTA transaction and
             *  calls EntityManager.joinTransaction.
             *  "
             */
            for(ExtendedEntityManager extendedEntityManager: entityManagers.values()) {
                extendedEntityManager.internalAssociateWithJtaTx();
            }
        }

    }

    /**
     * Pops the current SFSB invocation off the invocation call stack
     *
     * @return the entity manager map
     */
    public static Map<String, ExtendedEntityManager> popCall() {
        ArrayList<Map<String, ExtendedEntityManager>> stack = currentSFSBCallStack();
        Map<String, ExtendedEntityManager> result = stack.remove(stack.size() - 1);
        stack.trimToSize();
        return result;
    }

    /**
     * gets the current SFSB invocation off the invocation call stack
     *
     * @return the entity manager map
     */
    static Map<String, ExtendedEntityManager> getCurrentCall() {
        ArrayList<Map<String, ExtendedEntityManager>> stack = currentSFSBCallStack();
        Map<String, ExtendedEntityManager> result = null;
        if (stack != null) {
            result = stack.get(stack.size() - 1);
        }
        return result;
    }


    private static class SFSBCallStackThreadData {
        /**
         * Each thread will have its own list of SFSB invocations in progress.
         */
        private ArrayList<Map<String, ExtendedEntityManager>> invocationStack = new ArrayList<Map<String, ExtendedEntityManager>>();

        /**
         * During SFSB creation, track the injected extended persistence contexts
         */
        private Map<String, ExtendedEntityManager> creationTimeXPCRegistration = null;

        private SFSBInjectedXPCs creationTimeInjectedXPCs;
        /**
         * Track the SFSB bean injection nesting level.  Zero indicates the top level bean, one is the first level of SFSBs injected,
         * two is the second level of SFSBs injected...
         */
        private int creationBeanNestingLevel = 0;
    }

}
