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

package org.jboss.as.test.integration.ee.injection.resource.enventry.async;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A CompEnvInSessionSyncCallbacksUnitTestCase.
 * Part of migration AS5 testsuite to AS7 testsuite (JBQA-5275). 
 * 
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 */
@RunWith(Arquillian.class)
public class CompEnvInSessionSyncCallbacksUnitTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "compenv-sessionsync.jar").addPackage(
                CompEnvInSessionSyncCallbacksUnitTestCase.class.getPackage());
        jar.addAsManifestResource(CompEnvInSessionSyncCallbacksUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    /**
     * This test is based on command pattern and calling methods on the target stateful session bean from a delegating stateless
     * session bean instead of direct method invocations on the target stateful session bean. This is done on purpose to make
     * sure the container sets up comp/env properly in the transaction synchronization implementation before invoking
     * SessionSynchronization callbacks.
     * 
     * @throws Exception
     */
    @Test
    public void testBeforeCompletion() throws Exception {
        ActionExecutorHome executorHome = (ActionExecutorHome) ctx.lookup("java:module/ActionExecutor!"
                + ActionExecutorHome.class.getName());
        ActionExecutor executor = executorHome.create();

        StatefulSessionHome sessionHome = (StatefulSessionHome) ctx.lookup("java:module/StatefulSession!"
                + StatefulSessionHome.class.getName());
        final StatefulSession session = sessionHome.create();
        try {
            Assert.assertEquals("after-begin", executor.execute(new GetAfterBeginEntryAction(session.getHandle())));
            Assert.assertEquals("before-completion", executor.execute(new GetBeforeCompletionEntryAction(session.getHandle())));
            Assert.assertEquals("after-completion", executor.execute(new GetAfterCompletionEntryAction(session.getHandle())));
        } finally {
            if (session != null) {
                session.remove();
            }
        }
    }
}
