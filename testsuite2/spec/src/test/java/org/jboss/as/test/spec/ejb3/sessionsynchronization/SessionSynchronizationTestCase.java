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
import org.jboss.as.test.spec.ejb3.stateful.SFSBWithRemoveMethods;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
    public static JavaArchive deployment() {
        // create the ejb jar
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "sessionsynchronization.jar");
        jar.addPackage(SynchedStatefulBean.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    @EJB
    private SynchedStatefulBean bean;

    @Test
    public void testSync() {
        bean.doNothing();
        assertTrue(SynchedStatefulBean.afterBeginCalled);
        assertTrue(SynchedStatefulBean.beforeCompletionCalled);
        assertTrue(SynchedStatefulBean.afterCompletionCalled);
    }
}
