/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.timeout;

import jakarta.ejb.NoSuchEJBException;

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
     * Verifies that a stateful bean with timeout value 0 is eligible for removal immediately after creation and its
     * preDestroy method is invoked.
     */
    @Test
    public void testStatefulTimeout0() throws Exception {
        Annotated0TimeoutBean timeout0 = lookup(Annotated0TimeoutBean.class);
        try {
            timeout0.doStuff();
            throw new RuntimeException("Expecting NoSuchEJBException");
        } catch (NoSuchEJBException expected) {
        }
        Assert.assertTrue(Annotated0TimeoutBean.preDestroy);
    }

    /**
     * Verifies that a stateful bean with timeout value 0 is eligible for removal immediately and its
     * preDestroy method is invoked, but not during an active transaction.
     */
    @Test
    public void testTxStatefulTimeout0() throws Exception {
        this.userTransaction.begin();
        Annotated0TimeoutBean timeout0 = lookup(Annotated0TimeoutBean.class);
        // Invocation should succeed, since bean was created within an active transaction
        timeout0.doStuff();
        this.userTransaction.commit();
        try {
            timeout0.doStuff();
            throw new RuntimeException("Expecting NoSuchEJBException");
        } catch (NoSuchEJBException expected) {
        }
        Assert.assertTrue(Annotated0TimeoutBean.preDestroy);
    }
}
