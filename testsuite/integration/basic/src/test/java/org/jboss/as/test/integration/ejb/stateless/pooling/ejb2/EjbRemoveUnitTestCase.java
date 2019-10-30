/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
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

package org.jboss.as.test.integration.ejb.stateless.pooling.ejb2;

import java.rmi.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.wildfly.test.api.Authentication;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that bean is pooled and ejbRemove is correctly called.
 * Part of the migration of tests from EJB3 testsuite to AS7 testsuite [JBQA-5483].
 *
 * Dimitris Andreadis, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@ServerSetup(EjbRemoveUnitTestCase.EjbRemoveUnitTestCaseSetup.class)
public class EjbRemoveUnitTestCase {
    private static final Logger log = Logger.getLogger(EjbRemoveUnitTestCase.class.getName());

    private static final String POOL_NAME2 = "CustomConfig2";
    private static final String POOL_NAME3 = "CustomConfig3";
    private static final String DEFAULT_POOL_ATTR = "default-slsb-instance-pool";

    public static final CountDownLatch CDL = new CountDownLatch(10);

    static class EjbRemoveUnitTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            EJBManagementUtil.createStrictMaxPool(managementClient.getControllerClient(), POOL_NAME2, 5, 10 * 1000, TimeUnit.MILLISECONDS);
            EJBManagementUtil.createStrictMaxPool(managementClient.getControllerClient(), POOL_NAME3, 5, 10 * 1000, TimeUnit.MILLISECONDS);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            EJBManagementUtil.removeStrictMaxPool(managementClient.getControllerClient(), POOL_NAME2);
            EJBManagementUtil.removeStrictMaxPool(managementClient.getControllerClient(), POOL_NAME3);
        }
    }


    @ArquillianResource
    private InitialContext ctx;

    @ArquillianResource
    private ManagementClient managementClient;


    @Deployment(managed=true, testable = false, name = "single", order = 0)
    public static Archive<?> deploymentSingleton()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar")
                .addClasses(CounterSingleton.class);
        return jar;
    }

    @Deployment(managed=true, testable = true, name = "beans", order = 1)
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-session-remove.jar");
        jar.addClasses(
                CountedSessionHome.class, CountedSession.class,
                CountedSessionBean1.class, CountedSessionBean2.class, CountedSessionBean3.class,
                Authentication.class);
        jar.addAsManifestResource(EjbRemoveUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(EjbRemoveUnitTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(EjbRemoveUnitTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment.single.jar, org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        return jar;
    }

    /**
     * In this test, pooling is enabled (Maximum==5) so after the initial create() call, the same instance should be used from
     * the pool, and only removed when the app gets undeployed
     */
    @Test
    @OperateOnDeployment("beans")
    public void testEjbRemoveNotCalledForEveryCall() throws Exception {
        CountedSessionHome countedHome = (CountedSessionHome) ctx.lookup("java:module/CountedSession2!"
                + CountedSessionHome.class.getName());

        CountedSession counted = countedHome.create();
        counted.doSomething(2);
        Assert.assertEquals("createCounter", 1, CounterSingleton.createCounter2.get());
        Assert.assertEquals("removeCounter", 0, CounterSingleton.removeCounter2.get());
        counted.remove();
        Assert.assertEquals("createCounter", 1, CounterSingleton.createCounter2.get());
        Assert.assertEquals("removeCounter", 0, CounterSingleton.removeCounter2.get());
    }

    @Test
    @OperateOnDeployment("beans")
    public void testEjbRemoveMultiThread() throws Exception {
        CountedSessionHome countedHome = (CountedSessionHome) ctx.lookup("java:module/CountedSession3!"
                + CountedSessionHome.class.getName());

        final CountedSession counted = countedHome.create();

        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    // introduce 250ms delay
                    counted.doSomethingSync(233);
                } catch (RemoteException e) {
                    // ignore
                }
            }
        };

        for (int i = 0; i < 10; i++) {
            new Thread(runnable).start();
        }

        // wait for all 10 threads to finish
        CDL.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("createCounter has to be == 5 but was " + CounterSingleton.createCounter3.get(), CounterSingleton.createCounter3.get() == 5);
    }
}
