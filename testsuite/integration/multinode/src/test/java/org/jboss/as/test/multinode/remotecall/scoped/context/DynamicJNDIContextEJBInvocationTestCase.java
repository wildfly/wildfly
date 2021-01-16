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

package org.jboss.as.test.multinode.remotecall.scoped.context;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import java.io.FilePermission;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * A test case for testing the feature introduced in https://issues.jboss.org/browse/EJBCLIENT-34 which
 * allows applications to pass JNDI context properties during JNDI context creation for (scoped) EJB client
 * context creation
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup(PassivationConfigurationSetup.class)
public class DynamicJNDIContextEJBInvocationTestCase {

    private static final Logger logger = Logger.getLogger(DynamicJNDIContextEJBInvocationTestCase.class);

    private static final String LOCAL_DEPLOYMENT_NAME = "dynamic-jndi-context-ejb-invocation-test";

    private static final String REMOTE_SERVER_DEPLOYMENT_NAME = "deployment-on-other-server";

    @Deployment(name = "local-server-deployment")
    @TargetsContainer("multinode-client")
    public static Archive<?> createLocalDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, LOCAL_DEPLOYMENT_NAME + ".jar");
        jar.addClasses(StatefulBeanA.class, LocalServerStatefulRemote.class,PassivationConfigurationSetup.class, DynamicJNDIContextEJBInvocationTestCase.class, StatefulRemoteOnOtherServer.class, StatelessRemoteOnOtherServer.class);
        jar.addClasses(StatefulRemoteHomeForBeanOnOtherServer.class);
        jar.addAsManifestResource(DynamicJNDIContextEJBInvocationTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
        jar.addAsManifestResource(DynamicJNDIContextEJBInvocationTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(createPermissionsXmlAsset(
                new FilePermission(System.getProperty("jbossas.multinode.server") + "/standalone/tmp/auth/*", "read")),
                "permissions.xml"
        );
        return jar;
    }

    @Deployment(name = "remote-server-deployment", testable = false)
    @TargetsContainer("multinode-server")
    public static Archive<?> createDeploymentForRemoteServer() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, REMOTE_SERVER_DEPLOYMENT_NAME + ".jar");
        jar.addClasses(StatefulRemoteOnOtherServer.class, StatelessRemoteOnOtherServer.class, StatefulRemoteHomeForBeanOnOtherServer.class);
        jar.addClasses(StatefulBeanOnOtherServer.class, StatelessBeanOnOtherServer.class);
        return jar;
    }

    /**
     * Tests that a SFSB hosted on server X can lookup and invoke a SFSB and SLSB hosted on a different server,
     * by using a JNDI context which was created by passing the EJB client context creation properties
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("local-server-deployment")
    public void testServerToServerSFSBInvocation() throws Exception {
        final StatefulBeanA sfsbOnLocalServer = InitialContext.doLookup("java:module/" + StatefulBeanA.class.getSimpleName() + "!" + StatefulBeanA.class.getName());
        final int initialCount = sfsbOnLocalServer.getCountByInvokingOnRemoteServerBean();
        Assert.assertEquals("Unexpected initial count from stateful bean", 0, initialCount);
        // just increment a few times
        final int NUM_TIMES = 5;
        for (int i = 0; i < NUM_TIMES; i++) {
            sfsbOnLocalServer.incrementCountByInvokingOnRemoteServerBean();
        }
        final int countAfterIncrement = sfsbOnLocalServer.getCountByInvokingOnRemoteServerBean();
        Assert.assertEquals("Unexpected count after increment, from stateful bean", NUM_TIMES, countAfterIncrement);

        // let the SFSB invoke an SLSB on a remote server
        final String message = "foo";
        final String firstEcho = sfsbOnLocalServer.getEchoByInvokingOnRemoteServerBean(message);
        Assert.assertEquals("Unexpected echo from remote server SLSB", message, firstEcho);
    }

    /**
     * Tests that a SFSB Foo hosted on server X can lookup and store a SFSB and a SLSB hosted on a different server Y,
     * by using a JNDI context which was created by passing the EJB client context creation properties. The SFSB Foo
     * on server X is then allowed to passivate and after activation the invocations on the SFSB and SLSB members held
     * as state by SFSB Foo are expected to correcty end up on the remote server Y and return the correct state information
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("local-server-deployment")
    public void testSFSBPassivationWithScopedEJBProxyMemberInstances() throws Exception {
        final StatefulBeanA sfsbOnLocalServer = InitialContext.doLookup("java:module/" + StatefulBeanA.class.getSimpleName() + "!" + StatefulBeanA.class.getName());
        final int initialCount = sfsbOnLocalServer.getCountByInvokingOnRemoteServerBean();
        Assert.assertEquals("Unexpected initial count from stateful bean", 0, initialCount);
        // just increment a few times
        final int NUM_TIMES_BEFORE_PASSIVATION = 5;
        for (int i = 0; i < NUM_TIMES_BEFORE_PASSIVATION; i++) {
            sfsbOnLocalServer.incrementCountByInvokingOnRemoteServerBean();
        }
        final int countAfterIncrement = sfsbOnLocalServer.getCountByInvokingOnRemoteServerBean();
        Assert.assertEquals("Unexpected count after increment, from stateful bean", NUM_TIMES_BEFORE_PASSIVATION, countAfterIncrement);

        // let the SFSB invoke an SLSB on a remote server
        final String message = "foo";
        final String firstEcho = sfsbOnLocalServer.getEchoByInvokingOnRemoteServerBean(message);
        Assert.assertEquals("Unexpected echo from remote server SLSB", message, firstEcho);

        // now let's wait for passivation of the SFSB on local server
        final CountDownLatch passivationLatch = new CountDownLatch(1);
        sfsbOnLocalServer.registerPassivationNotificationLatch(passivationLatch);

        logger.trace("Triggering passivation of " + StatefulBeanA.class.getSimpleName() + " bean");
        InitialContext.doLookup("java:module/" + StatefulBeanA.class.getSimpleName() + "!" + StatefulBeanA.class.getName());

        final boolean passivated = passivationLatch.await(2, TimeUnit.SECONDS);
        if (passivated) {
            logger.trace("pre-passivate invoked on " + StatefulBeanA.class.getSimpleName() + " bean");
        } else {
            Assert.fail(sfsbOnLocalServer + " was not passivated");
        }
        // just wait a little while longer since the acknowledgement that the pre-passivate was invoked
        // doesn't mean the passivation process is complete
        Thread.sleep(1000);

        // let's activate the passivated SFSB on local server
        final int countAfterActivate = sfsbOnLocalServer.getCountByInvokingOnRemoteServerBean();
        Assert.assertEquals("Unexpected count from stateful bean after it was activated", NUM_TIMES_BEFORE_PASSIVATION, countAfterActivate);
        // just make sure @PostActivate was invoked
        Assert.assertTrue("Post-activate method was not invoked on bean " + StatefulBeanA.class.getSimpleName(), sfsbOnLocalServer.wasPostActivateInvoked());
        // now increment on the remote server SFSB via the local server SFSB
        final int NUM_TIMES_AFTER_ACTIVATION = 2;
        for (int i = 0; i < NUM_TIMES_AFTER_ACTIVATION; i++) {
            sfsbOnLocalServer.incrementCountByInvokingOnRemoteServerBean();
        }
        final int counterAfterIncrementOnPostActivate = sfsbOnLocalServer.getCountByInvokingOnRemoteServerBean();
        Assert.assertEquals("Unexpected count after increment, on the post activated stateful bean", NUM_TIMES_BEFORE_PASSIVATION + NUM_TIMES_AFTER_ACTIVATION, counterAfterIncrementOnPostActivate);

        // let's also invoke on the remote server SLSB via the local server SFSB
        final String echoAfterPostActivate = sfsbOnLocalServer.getEchoByInvokingOnRemoteServerBean(message);
        Assert.assertEquals("Unexpected echo message from remote server SLSB", message, echoAfterPostActivate);

    }

    /**
     * Tests that a SFSB hosted on server X can lookup and invoke a SFSB hosted on a different server, through the EJB 2.x
     * home view, by using a JNDI context which was created by passing the EJB client context creation properties
     * @see https://issues.jboss.org/browse/EJBCLIENT-51
     *
     * @throws Exception
     */
    @Test
    @OperateOnDeployment("local-server-deployment")
    public void testServerToServerSFSBUsingEJB2xHomeView() throws Exception {
        final StatefulBeanA sfsbOnLocalServer = InitialContext.doLookup("java:module/" + StatefulBeanA.class.getSimpleName() + "!" + StatefulBeanA.class.getName());
        final int countUsingEJB2xHomeView = sfsbOnLocalServer.getStatefulBeanCountUsingEJB2xHomeView();
        Assert.assertEquals("Unexpected initial count from stateful bean", 0, countUsingEJB2xHomeView);

        // now try the other create... method on the remote home
        final int countUsingEJB2xHomeViewDifferentWay = sfsbOnLocalServer.getStatefulBeanCountUsingEJB2xHomeViewDifferentWay();
        Assert.assertEquals("Unexpected initial count from stateful bean", 0, countUsingEJB2xHomeViewDifferentWay);

        // yet another create method
        final int initialCount = 54;
        final int countUsingEJB2xHomeViewYetAnotherWay = sfsbOnLocalServer.getStatefulBeanCountUsingEJB2xHomeViewYetAnotherWay(initialCount);
        Assert.assertEquals("Unexpected initial count from stateful bean", initialCount, countUsingEJB2xHomeViewYetAnotherWay);
    }

}
