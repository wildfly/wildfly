/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.spec.ejb3.sessionsynchronization;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class SessionSynchronizationTestCase {
    private static final Logger log = Logger.getLogger(SessionSynchronizationTestCase.class.getName());

    @Deployment
    public static Archive<?> deployment() {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "sessionsynchronization.war")
                .addPackage(SynchedStatefulBean.class.getPackage())
                .addPackage(SynchedStatefulBean2.class.getPackage())
                .addAsWebInfResource("sessionsynchronization/ejb-jar.xml", "ejb-jar.xml");
        log.info(war.toString(true));
        return war;
    }

    @EJB(mappedName = "java:global/sessionsynchronization/DescribedSynchedStatefulBean")
    private DescribedSynchedStatefulBean describedBean;

    @EJB(mappedName = "java:global/sessionsynchronization/SynchedStatefulBean")
    private SynchedStatefulBean bean;

    @EJB(mappedName = "java:global/sessionsynchronization/SynchedStatefulBean2")
    private SynchedStatefulBean2 bean2;

    @Test
    public void testDescriptor() {
        describedBean.doNothing();
        assertTrue(DescribedSynchedStatefulBean.afterBeginCalled);
        assertTrue(DescribedSynchedStatefulBean.beforeCompletionCalled);
        assertTrue(DescribedSynchedStatefulBean.afterCompletionCalled);
    }

    @Test
    public void testSessionSynchronizationInterface() {
        bean2.doNothing();
        assertTrue(SynchedStatefulBean2.afterBeginCalled);
        assertTrue(SynchedStatefulBean2.beforeCompletionCalled);
        assertTrue(SynchedStatefulBean2.afterCompletionCalled);
    }

    @Test
    public void testSync() {
        bean.doNothing();
        assertTrue(SynchedStatefulBean.afterBeginCalled);
        assertTrue(SynchedStatefulBean.beforeCompletionCalled);
        assertTrue(SynchedStatefulBean.afterCompletionCalled);
    }
}
