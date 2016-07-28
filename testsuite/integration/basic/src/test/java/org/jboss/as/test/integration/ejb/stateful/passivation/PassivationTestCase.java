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

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import javax.ejb.NoSuchEJBException;
import javax.naming.InitialContext;

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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests various scenarios for stateful bean passivation
 *
 * @author ALR, Stuart Douglas, Ondrej Chaloupka, Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup(PassivationTestCaseSetup.class)
public class PassivationTestCase {
    private static final Logger log = Logger.getLogger(PassivationTestCase.class);

    private static final long PASSIVATION_WAIT = TimeoutUtil.adjust(1000);

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
        jar.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "jboss-permissions.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    @Ignore("WFLY-6743")
    public void testPassivationMaxSize() throws Exception {
        PassivationInterceptor.reset();
        try (TestPassivationRemote remote1 = (TestPassivationRemote) ctx.lookup("java:module/" + TestPassivationBean.class.getSimpleName())) {
            Assert.assertEquals("Returned remote1 result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                    remote1.returnTrueString());
            remote1.addEntity(1, "Bob");
            remote1.setManagedBeanMessage("bar");
            Assert.assertTrue(remote1.isPersistenceContextSame());

            try (TestPassivationRemote remote2 = (TestPassivationRemote) ctx.lookup("java:module/" + TestPassivationBean.class.getSimpleName())) {
                Assert.assertEquals("Returned remote2 result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                        remote2.returnTrueString());
                Assert.assertTrue(remote2.isPersistenceContextSame());

                // create another bean. This should force the other bean to passivate, as only one bean is allowed in the pool at a time
                ctx.lookup("java:module/" + TestPassivationBean.class.getSimpleName());

                // Passivation happens asynchronously, so give it a sec
                Thread.sleep(PASSIVATION_WAIT);

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
            }
        }
        Assert.assertTrue("invalid: " + PassivationInterceptor.getPrePassivateTarget(), PassivationInterceptor.getPrePassivateTarget() instanceof TestPassivationBean);
        Assert.assertTrue("invalid: " + PassivationInterceptor.getPostActivateTarget(), PassivationInterceptor.getPostActivateTarget() instanceof TestPassivationBean);
    }

    /**
     * Tests that an EJB 3.2 stateful bean which is marked as <code>passivationCapable=false</code> isn't passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationDisabledBean() throws Exception {
        try (Bean bean = (Bean) ctx.lookup("java:module/" + PassivationDisabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
            bean.doNothing();
            // now do the same with a deployment descriptor configured stateful bean
            try (Bean ddBean = (Bean) ctx.lookup("java:module/passivation-disabled-bean" + "!" + Bean.class.getName())) {
                ddBean.doNothing();

                try (Bean bean2 = (Bean) ctx.lookup("java:module/" + PassivationDisabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
                    bean2.doNothing();
                    // now do the same with a deployment descriptor configured stateful bean
                    try (Bean ddBean2 = (Bean) ctx.lookup("java:module/passivation-disabled-bean" + "!" + Bean.class.getName())) {
                        ddBean2.doNothing();

                        // Passivation happens asynchronously, so give it a sec
                        Thread.sleep(PASSIVATION_WAIT);

                        // make sure bean's passivation and activation callbacks weren't invoked
                        Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly passivated", bean.wasPassivated());
                        Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly activated", bean.wasActivated());

                        Assert.assertFalse("(Deployment descriptor based) Stateful bean marked as passivation disabled was incorrectly passivated", ddBean.wasPassivated());
                        Assert.assertFalse("(Deployment descriptor based) Stateful bean marked as passivation disabled was incorrectly activated", ddBean.wasActivated());
                    }
                }
            }
        }
    }

    /**
     * Tests that an EJB 3.2 stateful bean which is marked as <code>passivationCapable=true</code> is passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationEnabledBean() throws Exception {
        try (Bean bean = (Bean) ctx.lookup("java:module/" + PassivationEnabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
            // make an invocation
            bean.doNothing();
            // now do the same with a deployment descriptor configured stateful bean
            try (Bean ddBean = (Bean) ctx.lookup("java:module/passivation-enabled-bean" + "!" + Bean.class.getName())) {
                ddBean.doNothing();

                // Create a 2nd set of beans, forcing the first set to passivate
                try (Bean bean2 = (Bean) ctx.lookup("java:module/" + PassivationEnabledBean.class.getSimpleName() + "!" + Bean.class.getName())) {
                    bean2.doNothing();
                    try (Bean ddBean2 = (Bean) ctx.lookup("java:module/passivation-enabled-bean" + "!" + Bean.class.getName())) {
                        ddBean2.doNothing();

                        // Passivation happens asynchronously, so give it a sec
                        Thread.sleep(PASSIVATION_WAIT);

                        Assert.assertTrue("(Annotation based) Stateful bean marked as passivation enabled was not passivated", bean.wasPassivated());
                        Assert.assertTrue("(Annotation based) Stateful bean marked as passivation enabled was not activated", bean.wasActivated());

                        Assert.assertTrue("(Deployment descriptor based) Stateful bean marked as passivation enabled was not passivated", ddBean.wasPassivated());
                        Assert.assertTrue("(Deployment descriptor based) Stateful bean marked as passivation enabled was not activated", ddBean.wasActivated());
                    }
                }
            }
        }
    }

    /**
     * Tests that an EJB 3.2 stateful bean which is marked as <code>passivationCapable=true</code> via annotation but overridden
     * as passivation disabled via deployment descriptor, isn't passivated or activated
     *
     * @throws Exception
     */
    @Test
    public void testPassivationDDOverrideBean() throws Exception {
        try (Bean passivationOverrideBean = (Bean) ctx.lookup("java:module/passivation-override-bean" + "!" + Bean.class.getName())) {
            // make an invocation
            passivationOverrideBean.doNothing();

            // Create a 2nd set of beans, that would normally force the first set to passivate
            try (Bean passivationOverrideBean2 = (Bean) ctx.lookup("java:module/passivation-override-bean" + "!" + Bean.class.getName())) {
                passivationOverrideBean2.doNothing();

                // Passivation happens asynchronously, so give it a sec
                Thread.sleep(PASSIVATION_WAIT);

                // make sure bean's passivation and activation callbacks weren't invoked
                Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly passivated", passivationOverrideBean.wasPassivated());
                Assert.assertFalse("(Annotation based) Stateful bean marked as passivation disabled was incorrectly activated", passivationOverrideBean.wasActivated());
            }
        }
    }

    /**
     * Tests passivation of bean that throws exception during serialization.
     */
    @Test
    public void testPassivationFailure() throws Exception {
        PassivationInterceptor.reset();

        // create first bean
        try (TestPassivationRemote remote1 = (TestPassivationRemote) ctx.lookup("java:module/" + BeanWithSerializationIssue.class.getSimpleName())) {
            // make an invocation
            Assert.assertEquals("Returned remote1 result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                    remote1.returnTrueString());

            // create second bean, this should force the first bean to passivate
            try (TestPassivationRemote remote2 = (TestPassivationRemote) ctx.lookup("java:module/" + BeanWithSerializationIssue.class.getSimpleName())) {
                // make an invocation
                Assert.assertEquals("Returned remote2 result was not expected", TestPassivationRemote.EXPECTED_RESULT,
                        remote2.returnTrueString());

                // passivation happens asynchronously, so give it a sec
                Thread.sleep(PASSIVATION_WAIT);

                // verify that remote1 was prePassivated
                Assert.assertTrue(PassivationInterceptor.getPrePassivateTarget() instanceof BeanWithSerializationIssue);
                Assert.assertTrue(((BeanWithSerializationIssue) PassivationInterceptor.getPrePassivateTarget())
                        .hasBeenPassivated());
                // verify that remote1 was not postActivated yet
                Assert.assertTrue(PassivationInterceptor.getPostActivateTarget() == null);

                // From EJB 4.2.1:
                // The container may destroy a session bean instance if the instance does not meet the requirements for serialization after PrePassivate.
                try {
                    // make another invocation on the first bean
                    remote1.returnTrueString();
                    Assert.fail("This invocation should not succeed since passivation failed");
                } catch (NoSuchEJBException e) {
                    // Expected
                }
            } catch (NoSuchEJBException e) {
                // Expected
            }
        } catch (NoSuchEJBException e) {
            // Expected
        }
    }
}
