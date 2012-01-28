/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.jboss.as.jpa.ejb3.SFSBContextHandleImpl;
import org.jboss.as.jpa.spi.SFSBContextHandle;
import org.junit.Test;

/**
 * @author Scott Marlow
 */
public class SFSBXPCMapTestCase {

    // target data structure that is being tested
    private SFSBXPCMap sfsbxpcMap = new SFSBXPCMap();

    // pu name represents the reference to the persistence unit that is used by each persistence context
    private static final String PU_NAME = "scopedPUName";

    // represents the stateful session bean (not a real bean but just equivalent to the real SFSB uuid that is
    // also serializable)
    private static final Serializable SFSB1 = "stateful session bean1";
    private static final Serializable SFSB2 = "stateful session bean2";
    private static final Serializable SFSB3 = "stateful session bean3";

    /**
     * Tests that we can lookup the extended persistence context as expected
     *
     * @throws Exception
     */
    @Test
    public void testLookup() throws Exception {

        SFSBContextHandle beanContextHandle1 = new SFSBContextHandleImpl(SFSB1);
        SFSBContextHandle beanContextHandle2 = new SFSBContextHandleImpl(SFSB2);
        SFSBContextHandle beanContextHandle3 = new SFSBContextHandleImpl(SFSB3);

        final EntityManager underlyingEntityManager = null;

        // the single extended persistence context is shared between the three stateful session beans
        ExtendedEntityManager extendedEntityManager = new ExtendedEntityManager(PU_NAME, underlyingEntityManager);

        assertEquals("the same ExtendedEntityManager equals method returns true for the same instance", extendedEntityManager, extendedEntityManager);

        // register the three beans
        sfsbxpcMap.register(beanContextHandle1, extendedEntityManager);
        Set<ExtendedEntityManager> xpcSet = sfsbxpcMap.getXPC(beanContextHandle1);
        ExtendedEntityManager gotXpc = xpcSet.iterator().next();
        assertEquals("SFSBXPCMap lookup of SFSB1 returns expected persistence context (SFSBXPCMap returned object=" +
            System.identityHashCode(gotXpc) +
            ", local extendedEntityManager =" + System.identityHashCode(extendedEntityManager) + ")",
            gotXpc, extendedEntityManager);

        sfsbxpcMap.register(beanContextHandle2, extendedEntityManager);
        xpcSet = sfsbxpcMap.getXPC(beanContextHandle2);
        gotXpc = xpcSet.iterator().next();
        assertEquals("SFSBXPCMap lookup of SFSB2 returns expected persistence context (SFSBXPCMap returned object=" +
            System.identityHashCode(gotXpc) +
            ", local extendedEntityManager =" + System.identityHashCode(extendedEntityManager) + ")",
            gotXpc, extendedEntityManager);

        sfsbxpcMap.register(beanContextHandle3, extendedEntityManager);
        xpcSet = sfsbxpcMap.getXPC(beanContextHandle3);
        gotXpc = xpcSet.iterator().next();
        assertEquals("SFSBXPCMap lookup of SFSB3 returns expected persistence context (SFSBXPCMap returned object=" +
            System.identityHashCode(gotXpc) +
            ", local extendedEntityManager =" + System.identityHashCode(extendedEntityManager) + ")",
            gotXpc, extendedEntityManager);

        List<SFSBContextHandle> list = sfsbxpcMap.getSFSBList(extendedEntityManager);
        assertNotNull("SFSBXPCMap.getSFSBList(entitymanager) returns non-null list of SFSB handles", list);

        assertTrue("SFSBXPCMap.getSFSBList(entitymanager) list contains SFSB1", list.contains(beanContextHandle1));
        assertTrue("SFSBXPCMap.getSFSBList(entitymanager) list contains SFSB2", list.contains(beanContextHandle2));
        assertTrue("SFSBXPCMap.getSFSBList(entitymanager) list contains SFSB3", list.contains(beanContextHandle3));
    }


}
