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

import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;

import org.jboss.as.jpa.spi.SFSBContextHandle;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * For stateful session bean life cycle management and tracking XPC Inheritance.
 * <p/>
 * There are two intended uses.
 * 1.  Getting the XPCs associated with a stateful session bean.
 * 2.  Getting the stateful session beans associated with a XPC.
 * <p/>
 * The XPC is represented by the EntityManager.
 *
 * @author Scott Marlow
 */
public class SFSBXPCMap {

    public static final AttachmentKey<SFSBXPCMap> ATTACHMENT_KEY = AttachmentKey.create(SFSBXPCMap.class);

    /**
     * Track the XPCs used by each stateful session bean.
     */
    private ConcurrentHashMap<SFSBContextHandle, List<EntityManager>> contextToXPCMap =
        new ConcurrentHashMap<SFSBContextHandle, List<EntityManager>>();


    /**
     * Track the stateful session beans that are referencing a XPC.
     */
    private ConcurrentHashMap<EntityManager, List<SFSBContextHandle>> XPCToContextMap =
        new ConcurrentHashMap<EntityManager, List<SFSBContextHandle>>();

    /**
     * Get the extended persistence contexts associate with the specified SFSB
     *
     * @param beanContextHandle represents the SFSB
     * @return a list of extended (XPC) persistence contexts
     */
    public List<EntityManager> getXPC(SFSBContextHandle beanContextHandle) {
        return contextToXPCMap.get(beanContextHandle);
    }

    /**
     * Get the stateful session beans associated with the specified XPC
     *
     * @param entityManager represents the extended persistence context (XPC)
     * @return list of stateful session beans
     */
    private List<SFSBContextHandle> getSFSB(EntityManager entityManager) {
        return XPCToContextMap.get(entityManager);
    }

    // at injection time, the SFSB that is being created isn't registered right away
    // that happens later at postConstruct time.
    //
    // The deferToPostConstruct is a one item length store (hack)
    private static ThreadLocal<List<EntityManager>> deferToPostConstruct = new ThreadLocal<List<EntityManager>>() {
        protected List<EntityManager> initialValue() {
            return new ArrayList<EntityManager>(1);
        }
    };

    /**
     * At injection time of a XPC, register the XPC (step 1 of 2)
     * finishRegistrationOfPersistenceContext is step 2
     *
     * @param xpc The ExtendedEntityManager
     */
    public static void registerPersistenceContext(EntityManager xpc) {

        if (xpc == null) {
            throw MESSAGES.nullParameter("SFSBXPCMap.RegisterPersistenceContext", "EntityManager");
        }

        if (!(xpc instanceof AbstractEntityManager)) {
            throw MESSAGES.parameterMustBeAbstractEntityManager("XPC");
        }

        List<EntityManager> store = deferToPostConstruct.get();
        store.add(xpc);
    }

    /**
     * Called by postconstruct interceptor
     */
    public void finishRegistrationOfPersistenceContext(SFSBContextHandle current) {
        List<EntityManager> store = deferToPostConstruct.get();
        for (EntityManager em : store) {
            register(current, (EntityManager) em);
        }
        store.clear();
    }

    /**
     * Register the specified stateful session bean with the XPC
     *
     * @param beanContextHandle represents the stateful session bean
     * @param entityManager     represents the extended persistence context (XPC)
     */
    public void register(SFSBContextHandle beanContextHandle, EntityManager entityManager) {
        if (!(entityManager instanceof AbstractEntityManager)) {
            throw MESSAGES.parameterMustBeAbstractEntityManager("XPC");
        }
        List<EntityManager> xpcList = contextToXPCMap.get(beanContextHandle);
        if (xpcList == null) {
            // create array of entity managers owned by a bean.  No synchronization is needed as it will only
            // be read/written to by one thread at a time (protected by the SFSB bean lock).
            xpcList = new ArrayList<EntityManager>();
            xpcList.add(entityManager);

            // no other thread should put at the same time on the same beanContextHandle
            Object existingList = contextToXPCMap.put(beanContextHandle, xpcList);
            if (existingList != null) {
                throw MESSAGES.multipleThreadsInvokingSfsb(beanContextHandle.getBeanContextHandle());
            }
        } else {
            // session bean was already registered, just add XPC to existing list.
            xpcList.add(entityManager);
        }

        // create array of stateful session beans that are sharing the entityManager
        // (entity manager should only be used locally to current thread)
        List<SFSBContextHandle> sfsbList = XPCToContextMap.get(entityManager);
        if (sfsbList == null) {
            sfsbList = new ArrayList<SFSBContextHandle>();
            Object existingList = XPCToContextMap.put(entityManager, sfsbList);
            if (existingList != null) {
                throw MESSAGES.multipleThreadsUsingEntityManager(entityManager);
            }
        }
        // XPC was already registered, just add SFSB to existing list.
        sfsbList.add(beanContextHandle);
    }

    /**
     * Remove the specified stateful session bean
     *
     * @param bean
     * @return a list of any XPCs that are ready to be closed by the caller (because the last SFSB was removed)
     */
    public List<EntityManager> remove(SFSBContextHandle bean) {
        List<EntityManager> result = null;
        // get list of extended persistence contexts that this bean was using.
        List<EntityManager> xpcList = contextToXPCMap.remove(bean);
        if (xpcList != null) {
            for (EntityManager xpc : xpcList) {
                List<SFSBContextHandle> sfsbList = XPCToContextMap.get(xpc);
                if (sfsbList != null) {
                    sfsbList.remove(bean);
                    if (sfsbList.size() == 0) {
                        if (result == null) {
                            result = new ArrayList<EntityManager>();
                        }
                        result.add(xpc);    // caller will close the xpc
                        XPCToContextMap.remove(xpc);
                    }
                }
            }
        }
        return result;
    }


    /**
     * Get or create a SFSBXPCMap that is shared over the top level deployment
     *
     * @param deploymentUnit
     * @return
     */
    public static SFSBXPCMap getXpcMap(final DeploymentUnit deploymentUnit) {
        final DeploymentUnit top = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        SFSBXPCMap sfsbMap = top.getAttachment(SFSBXPCMap.ATTACHMENT_KEY);
        if (sfsbMap == null) {
            synchronized (top) {
                sfsbMap = top.getAttachment(SFSBXPCMap.ATTACHMENT_KEY);
                if (sfsbMap == null) {
                    top.putAttachment(SFSBXPCMap.ATTACHMENT_KEY, sfsbMap = new SFSBXPCMap());
                }
            }
        }
        return sfsbMap;
    }


}
