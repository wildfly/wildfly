/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.sessionsynchronization;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

/**
 * Checks that SFSB SessionSynchronization callbacks do not allow concurrent access to the SFSB via the Transaction Reaper.
 * https://issues.jboss.org/browse/WFLY-6215
 *
 * @author Ryan Emerson
 */
@RunWith(Arquillian.class)
public class SessionSynchronizationSerializabilityTestCase {

    private static final String ARCHIVE_NAME = SessionSynchronizationSerializabilityTestCase.class.getName();

    @ArquillianResource
    private InitialContext ctx;

    @Inject
    private UserTransaction utx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(SessionSynchronizationSerializabilityTestCase.class.getPackage());
        return jar;
    }

    @Before
    public void resetBeanCounters() {
        SessionSynchBeanImpl.afterBegin.set(0);
        SessionSynchBeanImpl.afterCompletion.set(0);
    }

    @Test // Throws ConcurrentAccessException if callbacks are not serialized
    public void testSerializationOfBeanMethodsAndCallbacks() throws Exception {
        SessionSynchBeanImpl syncTestBean = lookup(SessionSynchBeanImpl.class);
        try {
            utx.setTransactionTimeout(5);
            utx.begin();
            for (int j = 0; j < 5; j++) {
                syncTestBean.method1();
                syncTestBean.method2();
            }
            utx.commit(); // Should never be reached
        } catch (EJBTransactionRolledbackException e) {
            utx.rollback();
            Assert.assertTrue(e.getMessage().startsWith("WFLYEJB0487"));
        }
        Assert.assertEquals(1, SessionSynchBeanImpl.afterBegin.get());
        Assert.assertEquals(1, SessionSynchBeanImpl.afterCompletion.get());
    }

    protected <T> T lookup(Class<T> beanType) throws NamingException {
        return beanType.cast(ctx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanType.getSimpleName() + "!" + beanType.getName()));
    }
}