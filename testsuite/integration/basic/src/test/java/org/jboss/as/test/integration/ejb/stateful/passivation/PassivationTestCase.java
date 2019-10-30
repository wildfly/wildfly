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

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
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

    @ArquillianResource
    private InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, PassivationTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(PassivationTestCase.class.getPackage());
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "jboss-permissions.xml");
        return jar;
    }

    @Test
    public void testPassivationMaxSize() throws Exception {
        PassivationInterceptor.reset();

        try (TestPassivationRemote remote1 = (TestPassivationRemote) ctx.lookup("java:module/" + TestPassivationBean.class.getSimpleName())) {
            Assert.assertEquals("Returned remote1 result was not expected", TestPassivationRemote.EXPECTED_RESULT, remote1.returnTrueString());
            remote1.addEntity(1, "Bob");
            remote1.setManagedBeanMessage("bar");

            Assert.assertTrue(remote1.isPersistenceContextSame());
            Assert.assertFalse("@PrePassivate called, check cache configuration and client sleep time", remote1.hasBeenPassivated());
            Assert.assertFalse("@PostActivate called, check cache configuration and client sleep time", remote1.hasBeenActivated());
            Assert.assertTrue(remote1.isPersistenceContextSame());
            Assert.assertEquals("Super", remote1.getSuperEmployee().getName());
            Assert.assertEquals("bar", remote1.getManagedBeanMessage());

            // create another bean. This should force the other bean to passivate, as only one bean is allowed in the cache at a time
            try (TestPassivationRemote remote2 = (TestPassivationRemote) ctx.lookup("java:module/" + TestPassivationBean.class.getSimpleName())) {
                Assert.assertEquals("Returned remote2 result was not expected", TestPassivationRemote.EXPECTED_RESULT, remote2.returnTrueString());

                Assert.assertTrue(remote2.isPersistenceContextSame());
                Assert.assertFalse("@PrePassivate called, check cache configuration and client sleep time", remote2.hasBeenPassivated());
                Assert.assertFalse("@PostActivate called, check cache configuration and client sleep time", remote2.hasBeenActivated());
                Assert.assertTrue(remote2.isPersistenceContextSame());
                Assert.assertEquals("Super", remote2.getSuperEmployee().getName());
                Assert.assertEquals("bar", remote2.getManagedBeanMessage());

                Assert.assertTrue("@PrePassivate not called, check cache configuration and client sleep time", remote1.hasBeenPassivated());
                Assert.assertTrue("@PostActivate not called, check cache configuration and client sleep time", remote1.hasBeenActivated());
                Assert.assertTrue(remote1.isPersistenceContextSame());
                Assert.assertEquals("Super", remote1.getSuperEmployee().getName());
                Assert.assertEquals("bar", remote1.getManagedBeanMessage());

                Assert.assertTrue("@PrePassivate not called, check cache configuration and client sleep time", remote2.hasBeenPassivated());
                Assert.assertTrue("@PostActivate not called, check cache configuration and client sleep time", remote2.hasBeenActivated());
                Assert.assertTrue(remote2.isPersistenceContextSame());
                Assert.assertEquals("Super", remote2.getSuperEmployee().getName());
                Assert.assertEquals("bar", remote2.getManagedBeanMessage());
            } finally {
                remote1.removeEntity(1);
            }
        }
        Assert.assertTrue("invalid: " + PassivationInterceptor.getPrePassivateTarget(), PassivationInterceptor.getPrePassivateTarget() instanceof TestPassivationBean);
        Assert.assertTrue("invalid: " + PassivationInterceptor.getPostActivateTarget(), PassivationInterceptor.getPostActivateTarget() instanceof TestPassivationBean);
        PassivationInterceptor.reset();
    }
}
