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

import static org.jboss.as.jpa.JpaLogger.ROOT_LOGGER;
import static org.jboss.as.jpa.JpaMessages.MESSAGES;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;

import org.jboss.as.jpa.ejb3.SFSBContextHandleImpl;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.jpa.spi.SFSBContextHandle;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceController;

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
    private ConcurrentHashMap<SFSBContextHandle, Set<ExtendedEntityManager>> contextToXPCMap =
        new ConcurrentHashMap<SFSBContextHandle, Set<ExtendedEntityManager>>();


    /**
     * Track the stateful session beans that are referencing a XPC.
     * Depends on the fact that ExtendedEntityManager implements ExtendedEntityManagerKey
     */
    private ConcurrentHashMap<ExtendedEntityManager, List<SFSBContextHandle>> XPCToContextMap =
        new ConcurrentHashMap<ExtendedEntityManager, List<SFSBContextHandle>>();

    /**
     * Get the extended persistence contexts associate with the specified SFSB
     *
     * @param beanContextHandle represents the SFSB
     * @return a list of extended (XPC) persistence contexts
     */
    public Set<ExtendedEntityManager> getXPC(SFSBContextHandle beanContextHandle) {
         return contextToXPCMap.get(beanContextHandle);
    }

    /**
     * Get the stateful session beans associated with the specified XPC
     *
     * @param entityManager represents the extended persistence context (XPC)
     * @return list of stateful session beans
     */
    public List<SFSBContextHandle> getSFSBList(ExtendedEntityManager entityManager) {
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

        if (!(xpc instanceof ExtendedEntityManager)) {
            throw MESSAGES.parameterMustBeExtendedEntityManager(xpc.getClass().getName());
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
            register(current, em);
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
        if (!(entityManager instanceof ExtendedEntityManager)) {
            throw MESSAGES.parameterMustBeExtendedEntityManager(entityManager.getClass().getName());
        }
        Set<ExtendedEntityManager> xpcSet = contextToXPCMap.get(beanContextHandle);
        if (xpcSet == null) {
            // create array of entity managers owned by a bean.  No synchronization is needed as it will only
            // be read/written to by one thread at a time (protected by the SFSB bean lock).
            xpcSet = new HashSet<ExtendedEntityManager>();
            xpcSet.add((ExtendedEntityManager)entityManager);

            // no other thread should put at the same time on the same beanContextHandle
            Object existingSet = contextToXPCMap.put(beanContextHandle, xpcSet);
            if (existingSet != null) {
                throw MESSAGES.multipleThreadsInvokingSfsb(beanContextHandle);
            }
        } else {
            // session bean was already registered, just add XPC to existing list.
            xpcSet.add((ExtendedEntityManager)entityManager);
        }

        // create array of stateful session beans that are sharing the entityManager
        // (entity manager should only be used locally to current thread)
        List<SFSBContextHandle> sfsbList = XPCToContextMap.get(entityManager);
        if (sfsbList == null) {
            sfsbList = new ArrayList<SFSBContextHandle>();
            Object existingList = XPCToContextMap.put((ExtendedEntityManager)entityManager, sfsbList);
            if (existingList != null) {
                throw MESSAGES.multipleThreadsUsingEntityManager(entityManager);}
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
        // get set of extended persistence contexts that this bean was using.
        Set<ExtendedEntityManager> xpcSet = contextToXPCMap.remove(bean);
        if (xpcSet != null) {
            for (ExtendedEntityManager xpc : xpcSet) {
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


    /**
     * Get a SFSBXPCMap that is shared over the top level deployment
     *
     * @param scopedPuName
     * @return
     */
    public static SFSBXPCMap getXpcMap(final String scopedPuName) {
        final ServiceController<?> controller = CurrentServiceContainer.getServiceContainer().getService(JPAServiceNames.getPUServiceName(scopedPuName));
        final PersistenceUnitServiceImpl persistenceUnitService = (PersistenceUnitServiceImpl)controller.getService();
        return persistenceUnitService.getSfsbxpcMap();
    }

    /**
     * help serialize an extended persitence context, by handling the serializing of the SFSBXPCMap state
     *
     * @param out
     * @param extendedEntityManager
     * @param puScopedName
     */
    protected static void delegateWriteObject(ObjectOutputStream out, ExtendedEntityManager extendedEntityManager, String puScopedName) throws
        IOException {

        boolean isPassivating = false;  // TODO: AS7-3388 need a way for ejb3 to tell me when we are cluster replicating
                                        //       versus passivating a stateful bean.

        SFSBXPCMap sfsbxpcMap = SFSBXPCMap.getXpcMap(puScopedName);
        List<SFSBContextHandle> sfsbList = sfsbxpcMap.getSFSBList(extendedEntityManager);
        ROOT_LOGGER.tracef("starting serializing of %d SFSBXPCMap entries", sfsbList.size());
        out.writeInt(sfsbList.size());  // write the count of SFSBContextHandle that reference extendedEntityManager
        for (SFSBContextHandle sfsbContextHandle : sfsbList) {
            out.writeObject(sfsbContextHandle.getSerializable());
            ROOT_LOGGER.tracef("serialized SFSBXPCMap entry %s", sfsbContextHandle.getSerializable().toString());
        }

        if (isPassivating) {

            sfsbList = sfsbxpcMap.getSFSBList(extendedEntityManager);

            // when we activate the SFSB, we will remap each SFSB to XPC
            for (SFSBContextHandle sfsbContextHandle : sfsbList) {
                Set XPCSet = sfsbxpcMap.contextToXPCMap.get(sfsbContextHandle);
                if (XPCSet != null) {
                    XPCSet.remove(extendedEntityManager);
                }
            }

            // when we activate, we will re-add with a new XPC instance
            sfsbxpcMap.XPCToContextMap.remove(extendedEntityManager);


        }
    }

    /**
     * help deserialize an extended persistence context, by handling the deserialization of the SFSBXPCMap state.
     * <p/>
     * As per contract between ejb3 clustering and extended persistence context clustering, Derserialization only
     * happens on an node where the SFSB serialization group is either active already or being activated.
     * <p/>
     * There are twp different cases to handle.  Cluster replication, followed by fail-over and
     * stateful session bean passivation followed by activation.
     * <p/>
     * For cluster replication, we are creating the SFSBXPCMap state related to the passed extended persistence
     * context.
     *
     * @param in
     * @param extendedEntityManager
     * @param puScopedName
     * @throws IOException
     */
    protected static void delegateReadObject(ObjectInputStream in, ExtendedEntityManager extendedEntityManager, String puScopedName) throws
        IOException {

        SFSBXPCMap sfsbxpcMap = SFSBXPCMap.getXpcMap(puScopedName);
        int sfsbContextHandleCount = in.readInt();
        ROOT_LOGGER.tracef("starting deserializing of %d SFSBXPCMap entries", sfsbContextHandleCount);
        ArrayList sfsbList = new ArrayList<SFSBContextHandle>();

        for (int looper = 0; looper < sfsbContextHandleCount; looper++) {
            try {
                Serializable sfsbContextHandleId = (Serializable) in.readObject();
                ROOT_LOGGER.tracef("deserialized SFSBXPCMap entry %s", sfsbContextHandleId.toString());
                SFSBContextHandleImpl sfsbContextHandle = new SFSBContextHandleImpl(sfsbContextHandleId);
                sfsbList.add(sfsbContextHandle);

                Set<ExtendedEntityManager> existingXPCSet = sfsbxpcMap.contextToXPCMap.get(extendedEntityManager);
                if (existingXPCSet == null) {
                    existingXPCSet = new HashSet<ExtendedEntityManager>();
                    sfsbxpcMap.contextToXPCMap.put(sfsbContextHandle, existingXPCSet);
                }
                existingXPCSet.add(extendedEntityManager);  // replace the current XPC (if any) with the deserialized instance

            } catch (ClassNotFoundException e) {
                throw MESSAGES.couldNotDeserialize(e, extendedEntityManager.getScopedPuName());
            }
        }
        sfsbxpcMap.XPCToContextMap.put(extendedEntityManager, sfsbList);  // replace the current XPC (if any) with the deserialized instance

    }

}
