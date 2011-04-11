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
import org.jboss.util.collection.ConcurrentReferenceHashMap;

import javax.persistence.EntityManager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private static final SFSBXPCMap INSTANCE = new SFSBXPCMap();

    public static SFSBXPCMap getINSTANCE() {
        return INSTANCE;
    }

    /**
     * Track the XPCs used by each stateful session bean.
     * If the only reference to the EntityManager is from here, it must not exist anymore (so it will be
     * removed from here.
     * <p/>
     * TODO:  The SFSB clustering should be careful not to create the situation where there are no
     * references to the EntityManager on a remote node (the SFSB should replicate before the SFSXPCMap entry for it.)
     * <p/>
     * Note:  We shouldn't have the case where the EntityManager list being looked up,
     * is ever null (since SFSBs should have strong references to the EntityManagers).
     * TODO:  After switching to system lifecycle callbacks, we won't need to rely on the GC,
     * so be ready to update this datastructure.
     */
    private ConcurrentReferenceHashMap<SFSBContextHandle, List<WeakReference<EntityManager>>> contextToXPCMap =
        new ConcurrentReferenceHashMap<SFSBContextHandle, List<WeakReference<EntityManager>>>
            (ConcurrentReferenceHashMap.ReferenceType.WEAK, ConcurrentReferenceHashMap.ReferenceType.STRONG);

    /**
     * Track the stateful session beans that are referencing a XPC.
     * <p/>
     * Note:  We shouldn't have the case where the EntityManager being looked up, is not referenced by a SFSB.
     * Therefore, the EntityManager shouldn't normally be null.
     */
    private ConcurrentReferenceHashMap<EntityManager, List<SFSBContextHandle>> XPCToContextMap =
        new ConcurrentReferenceHashMap<EntityManager, List<SFSBContextHandle>>
            (ConcurrentReferenceHashMap.ReferenceType.WEAK, ConcurrentReferenceHashMap.ReferenceType.STRONG);

    /**
     * Get the extended persistence contexts associate with the specified SFSB
     *
     * @param beanContextHandle represents the SFSB
     * @return a list of extended (XPC) persistence contexts
     */
    public List<WeakReference<EntityManager>> getXPC(SFSBContextHandle beanContextHandle) {
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
    private static ThreadLocal<Object[]> deferToPostConstruct = new ThreadLocal() {
        protected Object initialValue() {
            return new Object[1];
        }

    };

    /**
     * At injection time of a XPC, register the XPC (step 1 of 2)
     * finishRegistrationOfPersistenceContext is step 2
     *
     * @param xpc The ExtendedEntityManager
     */
    public static void RegisterPersistenceContext(EntityManager xpc) {

        if (xpc == null) {
            throw new RuntimeException("internal SFSBXPCMap.RegisterPersistenceContext error, null EntityManager passed in");
        }

        if (!(xpc instanceof AbstractEntityManager)) {
            throw new RuntimeException("internal error, XPC needs to be a AbstractEntityManager so that we can get metadata");
        }

        Object[] store = deferToPostConstruct.get();
        store[0] = xpc;
    }

    /**
     * Called by postconstruct interceptor
     */
    public void finishRegistrationOfPersistenceContext(SFSBContextHandle current) {
        Object[] store = deferToPostConstruct.get();
        if (store != null && store.length == 1) {
            if (store[0] == null) {
                throw new RuntimeException("internal SFSBXPCMap.finishRegistrationOfPersistenceContext error, null EntityManager passed in");
            }
            register(current, (EntityManager) store[0]);
            store[0] = null;    // clear store
        }
    }

    /**
     * Register the specified stateful session bean with the XPC
     *
     * @param beanContextHandle represents the stateful session bean
     * @param entityManager     represents the extended persistence context (XPC)
     */
    public void register(SFSBContextHandle beanContextHandle, EntityManager entityManager) {
        if (!(entityManager instanceof AbstractEntityManager)) {
            throw new RuntimeException("internal error, XPC needs to be a AbstractEntityManager so that we can get metadata");
        }
        List<WeakReference<EntityManager>> xpcList = contextToXPCMap.get(beanContextHandle);
        if (xpcList == null) {
            // create array of entity managers owned by a bean.  No synchronization is needed as it will only
            // be read/written to by one thread at a time (protected by the SFSB bean lock).
            xpcList = new ArrayList<WeakReference<EntityManager>>();
            xpcList.add(new WeakReference(entityManager));

            // no other thread should put at the same time on the same beanContextHandle
            Object existingList = contextToXPCMap.put(beanContextHandle, xpcList);
            if (existingList != null) {
                throw new RuntimeException("More than one thread is invoking stateful session bean '" +
                    beanContextHandle.getBeanContextHandle() + "' at the same time." );
            }
        } else {
            // session bean was already registered, just add XPC to existing list.
            xpcList.add(new WeakReference(entityManager));
        }


        // create array of stateful session beans that are sharing the entityManager
        List<SFSBContextHandle> sfsbList = Collections.synchronizedList(new ArrayList<SFSBContextHandle>());
        sfsbList.add(beanContextHandle);
        sfsbList = XPCToContextMap.putIfAbsent(entityManager, sfsbList);
        if (null != sfsbList) {
            // XPC was already registered, just add SFSB to existing list.
            sfsbList.add(beanContextHandle);
        }
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
        List<WeakReference<EntityManager>> xpcList = contextToXPCMap.remove(bean);
        if (xpcList != null) {
            for (WeakReference<EntityManager> xpc_ref : xpcList) {
                EntityManager xpc = xpc_ref.get();
                if (xpc != null) {  // null means the SFSB was destroyed without SFSBDestroyInterceptor triggering
                                    // we can ignore that case since the XPC is already closed by GC
                    List<SFSBContextHandle> sfsbList = XPCToContextMap.get(xpc);
                    if (sfsbList != null) {
                        // build a list of SFSBs that should be removed
                        List<SFSBContextHandle> removed = new ArrayList<SFSBContextHandle>();
                        for (SFSBContextHandle beanContextHandle : sfsbList) {
                            if (beanContextHandle == bean || beanContextHandle.getBeanContextHandle() == null) {
                                removed.add(beanContextHandle);
                            }
                        }
                        sfsbList.removeAll(removed);  // TODO:  delete this when we have system level callback
                        if (sfsbList.size() == 0) {
                            result.add(xpc);    // caller should close the xpc
                        }
                    }
                }

            }
        }
        return result;
    }


}
