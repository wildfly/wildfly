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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * For stateful session bean life cycle management and tracking XPC Inheritance.
 *
 * There are two intended uses.
 * 1.  Getting the XPCs associated with a stateful session bean.
 * 2.  Getting the stateful session beans associated with a XPC.
 *
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
     */
    private ConcurrentHashMap<SFSBContextHandle, List<EntityManager>> contextToXPCMap =
        new ConcurrentHashMap<SFSBContextHandle, List<EntityManager>>();

    /**
     * Track the stateful session beans that are referencing a XPC.
     *
     */
    private ConcurrentHashMap<EntityManager, List<SFSBContextHandle>> XPCToContextMap =
        new ConcurrentHashMap<EntityManager, List<SFSBContextHandle>>();

    /**
     * Get the extended persistence contexts associate with the specified SFSB
     * @param beanContextHandle represents the SFSB
     * @return a list of extended (XPC) persistence contexts
     */
    public List<EntityManager> getXPC(SFSBContextHandle beanContextHandle) {
        return contextToXPCMap.get(beanContextHandle);
    }

    /**
     * Get the stateful session beans associated with the specified XPC
     * @param entityManager represents the extended persistence context (XPC)
     * @return list of stateful session beans
     */
    public List<SFSBContextHandle> getSFSB(EntityManager entityManager) {
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
     *
     * TODO:  need a way to clear the XPC from ThreadLocal if step 2 doesn't happen (due to error)
     */
    public static void RegisterPersistenceContext(EntityManager xpc) {
        Object[] store = deferToPostConstruct.get();
        store[0] = xpc;
    }

    /**
     * Called by postconstruct interceptor
     */
    public void finishRegistrationOfPersistenceContext(SFSBContextHandle current) {
        Object[] store = deferToPostConstruct.get();
        if (store !=null && store.length == 1) {
            register(current, (EntityManager)store[0]);
            store[0] = null;    // clear store
        }
    }

    /**
     * Register the specified stateful session bean with the XPC
     * @param beanContextHandle represents the stateful session bean
     * @param entityManager represents the extended persistence context (XPC)
     */
    public void register(SFSBContextHandle beanContextHandle, EntityManager entityManager) {
        if (! (entityManager instanceof AbstractEntityManager)) {
            throw new RuntimeException("internal error, XPC needs to be a AbstractEntityManager so that we can get metadata");
        }
        List<EntityManager> xpcList = Collections.synchronizedList(new ArrayList<EntityManager>());
        xpcList.add(entityManager);
        xpcList = contextToXPCMap.putIfAbsent(beanContextHandle, xpcList);
        if( null != xpcList) {
            // session bean was already registered, just add XPC to existing list.
            xpcList.add(entityManager);
        }

        List<SFSBContextHandle> sfsbList = Collections.synchronizedList(new ArrayList<SFSBContextHandle>());
        sfsbList.add(beanContextHandle);
        sfsbList = XPCToContextMap.putIfAbsent(entityManager,sfsbList);
        if( null != sfsbList) {
            // XPC was already registered, just add SFSB to existing list.
            sfsbList.add(beanContextHandle);
        }
    }

    /**
     * Remove the specified stateful session bean
     * @param bean
     * @return a list of any XPCs that are ready to be closed by the caller (because the last SFSB was removed)
     */
    public List<EntityManager> remove(SFSBContextHandle bean) {
        List<EntityManager> result = null;
        List<EntityManager> xpcList = contextToXPCMap.remove(bean);
        if (xpcList != null) {
            for(EntityManager xpc:xpcList) {
                List<SFSBContextHandle> sfsbList = XPCToContextMap.get(xpc);
                if (sfsbList != null) {
                    // build a list of SFSBs that should be removed
                    List<SFSBContextHandle>removed = new ArrayList<SFSBContextHandle>();
                    for (SFSBContextHandle beanContextHandle:sfsbList) {
                        if (beanContextHandle == bean || beanContextHandle.getBeanContextHandle() == null) {
                            removed.add(beanContextHandle);
                        }
                    }
                    sfsbList.removeAll(removed);
                    if (sfsbList.size() == 0) {
                        result.add(xpc);    // caller should close the xpc
                    }
                }
            }
        }
        return result;
    }


}
