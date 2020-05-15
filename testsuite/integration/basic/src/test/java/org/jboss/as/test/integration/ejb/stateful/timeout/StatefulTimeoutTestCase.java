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

package org.jboss.as.test.integration.ejb.stateful.timeout;

import javax.ejb.NoSuchEJBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the stateful timeout annotation and deployment descriptor elements works.
 * Tests in this class do not configure default-stateful-bean-session-timeout or any other custom server setup.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class StatefulTimeoutTestCase extends StatefulTimeoutTestBase1 {
    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(StatefulTimeoutTestBase1.class.getPackage());
        jar.add(new StringAsset(DEPLOYMENT_DESCRIPTOR_CONTENT), "META-INF/ejb-jar.xml");
        jar.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        return jar;
    }

    @Test
    public void testStatefulTimeoutFromAnnotation() throws Exception {
        super.testStatefulTimeoutFromAnnotation();
    }

    @Test
    public void testStatefulTimeoutWithPassivation() throws Exception {
        super.testStatefulTimeoutWithPassivation();
    }

    @Test
    public void testStatefulTimeoutFromDescriptor() throws Exception {
        super.testStatefulTimeoutFromDescriptor();
    }

    @Test
    public void testStatefulBeanNotDiscardedWhileInTransaction() throws Exception {
        super.testStatefulBeanNotDiscardedWhileInTransaction();
    }

    @Test
    public void testStatefulBeanWithPassivationNotDiscardedWhileInTransaction() throws Exception {
        super.testStatefulBeanWithPassivationNotDiscardedWhileInTransaction();
    }

    /**
     * Verifies that a stateful bean that does not configure stateful timeout anywhere will not be timeout or to be removed.
     */
    @Test
    public void testNoTimeout() throws Exception {
        TimeoutNotConfiguredBean timeoutNotConfiguredBean = lookup(TimeoutNotConfiguredBean.class);
        timeoutNotConfiguredBean.doStuff();
        Thread.sleep(3000);
        timeoutNotConfiguredBean.doStuff();
        Assert.assertFalse(TimeoutNotConfiguredBean.preDestroy);
    }

    /**
     * Verifies that a stateful bean with timeout value 0 is eligible for removal immediately, and its
     * preDestroy method is invoked.
     */
    @Test
    public void testStatefulTimeout0() throws Exception {
        Annotated0TimeoutBean timeout0 = lookup(Annotated0TimeoutBean.class);
        timeout0.doStuff();
        try {
            timeout0.doStuff();
            throw new RuntimeException("Expecting NoSuchEJBException");
        } catch (NoSuchEJBException expected) {
        }
        Assert.assertTrue(Annotated0TimeoutBean.preDestroy);
    }
}
