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

package org.jboss.as.test.integration.ejb.stateful.passivation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * Tests various scenarios for stateful bean passivation
 *
 * @author ALR, Stuart Douglas, Ondrej Chaloupka, Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup(PassivationTestCaseSetup.class)
public class PassivationTestCase {
    private static final Logger log = Logger.getLogger(PassivationTestCase.class);

    @ArquillianResource
    private InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "passivation-test.jar");
        jar.addPackage(PassivationTestCase.class.getPackage());
        jar.addClass(TimeoutUtil.class);
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"),
                "MANIFEST.MF");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testPassivationMaxSize() throws Exception {
        PassivationInterceptor.reset();
        TestPassivationRemote remote1 = (TestPassivationRemote) ctx.lookup("java:module/"
                + TestPassivationBean.class.getSimpleName());
        Assert.assertEquals("Returned remote1 result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                remote1.returnTrueString());
        remote1.addEntity(1, "Bob");
        remote1.setManagedBeanMessage("bar");
        Assert.assertTrue(remote1.isPersistenceContextSame());

        TestPassivationRemote remote2 = (TestPassivationRemote) ctx.lookup("java:module/"
                + TestPassivationBean.class.getSimpleName());
        Assert.assertEquals("Returned remote2 result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                remote2.returnTrueString());
        Assert.assertTrue(remote2.isPersistenceContextSame());

        // create another bean. This should force the other bean to passivate, as only one bean is allowed in the pool at a time
        ctx.lookup("java:module/" + TestPassivationBean.class.getSimpleName());

        Assert.assertTrue("@PrePassivate not called, check cache configuration and client sleep time",
                remote1.hasBeenPassivated());
        Assert.assertTrue("@PrePassivate not called, check cache configuration and client sleep time",
                remote2.hasBeenPassivated());
        Assert.assertTrue(remote1.isPersistenceContextSame());
        Assert.assertTrue(remote2.isPersistenceContextSame());
        Assert.assertEquals("Super", remote1.getSuperEmployee().getName());
        Assert.assertEquals("Super", remote2.getSuperEmployee().getName());
        Assert.assertEquals("bar", remote1.getManagedBeanMessage());
        Assert.assertEquals("bar", remote2.getManagedBeanMessage());

        remote1.remove();
        remote2.remove();
        Assert.assertTrue("invalid: " + PassivationInterceptor.getPrePassivateTarget(), PassivationInterceptor.getPrePassivateTarget() instanceof TestPassivationBean);
        Assert.assertTrue("invalid: " + PassivationInterceptor.getPostActivateTarget(), PassivationInterceptor.getPostActivateTarget() instanceof TestPassivationBean);
    }

    @Test
    public void testPassivationIdleTimeout() throws Exception {
        PassivationInterceptor.reset();
        // Lookup and create stateful instance
        TestPassivationRemote remote = (TestPassivationRemote) ctx.lookup("java:module/"
                + TestPassivationBean.class.getSimpleName());
        // Make an invocation
        Assert.assertEquals("Returned result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                remote.returnTrueString());
        // Sleep, allow SFSB to passivate
        Thread.sleep(1600L);
        // Make another invocation
        Assert.assertEquals("Returned result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                remote.returnTrueString());
        // Ensure that @PostActivate was called during client sleep
        Assert.assertTrue("@PostActivate not called, check CacheConfig and client sleep time", remote.hasBeenActivated());
        // Ensure that @PrePassivate was called during the client sleep
        Assert.assertTrue("@PrePassivate not called, check CacheConfig and client sleep time", remote.hasBeenPassivated());
        remote.remove();
        Assert.assertTrue(PassivationInterceptor.getPostActivateTarget() instanceof TestPassivationBean);
        Assert.assertTrue(PassivationInterceptor.getPrePassivateTarget() instanceof TestPassivationBean);
    }

    /**
     * Tests that a EJB 3.2 stateful bean which is marked as <code>passivationCapable=false</code> isn't passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationDisabledBean() throws Exception {
        final PassivationDisabledBean bean = (PassivationDisabledBean) ctx.lookup("java:module/" + PassivationDisabledBean.class.getSimpleName() + "!" + PassivationDisabledBean.class.getName());
        // make an invocation
        bean.doNothing();
        // now do the same with a deployment descriptor configured stateful bean
        final DDBasedSFSB ddBean = (DDBasedSFSB) ctx.lookup("java:module/passivation-disabled-bean" + "!" + DDBasedSFSB.class.getName());
        ddBean.doNothing();

        // now wait for the passivation timeout to come into picture. Our test setup is configured to setup a passivating store with
        // idle timeout = 1 second
        final int sleepTime = TimeoutUtil.adjust(2000);
        Thread.sleep(sleepTime);
        // now invoke on the bean again
        bean.doNothing();
        ddBean.doNothing();

        // make sure bean's passivation and activation callbacks weren't invoked
        Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly passivated", bean.wasPassivated());
        Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly activated", bean.wasActivated());

        Assert.assertFalse("(Deployment descriptor based) Stateful bean marked as passivation disabled was incorrectly passivated", ddBean.wasPassivated());
        Assert.assertFalse("(Deployment descriptor based) Stateful bean marked as passivation disabled was incorrectly activated", ddBean.wasActivated());

    }

    /**
     * Tests that a EJB 3.2 stateful bean which is marked as <code>passivationCapable=true</code> is passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationEnabledBean() throws Exception {
        final PassivationEnabledBean bean = (PassivationEnabledBean) ctx.lookup("java:module/" + PassivationEnabledBean.class.getSimpleName() + "!" + PassivationEnabledBean.class.getName());
        // make an invocation
        bean.doNothing();
        // now do the same with a deployment descriptor configured stateful bean
        final DDBasedSFSB ddBean = (DDBasedSFSB) ctx.lookup("java:module/passivation-enabled-bean" + "!" + DDBasedSFSB.class.getName());
        ddBean.doNothing();
        // now wait for the passivation timeout to come into picture. Our test setup is configured to setup a passivating store with
        // idle timeout = 1 second
        final int sleepTime = TimeoutUtil.adjust(2000);
        Thread.sleep(sleepTime);
        // now invoke on the bean again
        bean.doNothing();
        ddBean.doNothing();

        // make sure bean's passivation and activation callbacks were invoked
        Assert.assertTrue("(Annotation based) Stateful bean marked as passivation enabled was not passivated", bean.wasPassivated());
        Assert.assertTrue("(Annotation based) Stateful bean marked as passivation enabled was not activated", bean.wasActivated());

        Assert.assertTrue("(Deployment descriptor based) Stateful bean marked as passivation enabled was not passivated", ddBean.wasPassivated());
        Assert.assertTrue("(Deployment descriptor based) Stateful bean marked as passivation enabled was not activated", ddBean.wasActivated());

    }

    /**
     * Tests that a EJB 3.2 stateful bean which is marked as <code>passivationCapable=true</code> via annotation but overridden
     * as passivation disabled via deployment descriptor, isn't passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationDDOverrideBean() throws Exception {
        final PassivationEnabledBean passivationOverrideBean = (PassivationEnabledBean) ctx.lookup("java:module/passivation-override-bean" + "!" + PassivationEnabledBean.class.getName());
        // make an invocation
        passivationOverrideBean.doNothing();
        // now wait for the passivation timeout to come into picture. Our test setup is configured to setup a passivating store with
        // idle timeout = 1 second
        final int sleepTime = TimeoutUtil.adjust(2000);
        Thread.sleep(sleepTime);
        // now invoke on the bean again
        passivationOverrideBean.doNothing();

        // make sure bean's passivation and activation callbacks weren't invoked
        Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly passivated", passivationOverrideBean.wasPassivated());
        Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly activated", passivationOverrideBean.wasActivated());

    }
}
