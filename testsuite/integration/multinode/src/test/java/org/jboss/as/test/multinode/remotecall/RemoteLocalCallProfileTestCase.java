/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.multinode.remotecall;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import javax.ejb.EJBException;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing multinode communication - calling Stateless and Stateful beans (via remote and local and home interfaces).
 *
 * @author William DeCoste, Ondrej Chaloupka
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(RemoteLocalCallProfileTestCase.RemoteLocalCallProfileTestCaseServerSetup.class)
public class RemoteLocalCallProfileTestCase {
    private static final Logger log = Logger.getLogger(RemoteLocalCallProfileTestCase.class);
    public static final String ARCHIVE_NAME_CLIENT = "remotelocalcall-test-client";
    public static final String ARCHIVE_NAME_SERVER = "remotelocalcall-test-server";

    static class RemoteLocalCallProfileTestCaseServerSetup implements ServerSetupTask {

        private static final PathAddress ADDR_REMOTING_PROFILE = PathAddress.pathAddress().append(SUBSYSTEM, "ejb3").append("remoting-profile", "test-profile");
        private static final PathAddress ADDR_REMOTING_EJB_RECEIVER = ADDR_REMOTING_PROFILE.append("remoting-ejb-receiver", "test-receiver");

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // /subsystem=ejb3/remoting-profile=test-profile:add()
            ModelNode remotingProfileAddModelNode = Util.createAddOperation(ADDR_REMOTING_PROFILE);
            steps.add(remotingProfileAddModelNode);

            // /subsystem=ejb3/remoting-profile=test-profile/remoting-ejb-receiver=test-receiver:add(outbound-connection-ref=remote-ejb-connection)
            ModelNode ejbReceiverAddModelNode = Util.createAddOperation(ADDR_REMOTING_EJB_RECEIVER);
            ejbReceiverAddModelNode.get("outbound-connection-ref").set("remote-ejb-connection");
            steps.add(ejbReceiverAddModelNode);

            Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            ModelNode remotingProfileRemoveModelNode = Util.createRemoveOperation(ADDR_REMOTING_PROFILE);

            Utils.applyUpdates(Collections.singletonList(remotingProfileRemoveModelNode), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }
    }

    @BeforeClass
    public static void printSysProps() {
        log.trace("System properties:\n" + System.getProperties());
    }

    @Deployment(name = "server")
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment0() {
        JavaArchive jar = createJar(ARCHIVE_NAME_SERVER);
        return jar;
    }

    @Deployment(name = "client")
    @TargetsContainer("multinode-client")
    public static Archive<?> deployment1() {
        JavaArchive jar = createJar(ARCHIVE_NAME_CLIENT);
        jar.addClasses(RemoteLocalCallProfileTestCase.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client-profile.xml", "jboss-ejb-client.xml");
        return jar;
    }

    private static JavaArchive createJar(String archiveName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, archiveName + ".jar");
        jar.addClasses(StatefulBean.class, StatefulLocal.class, StatefulLocalHome.class, StatefulRemote.class,
                StatefulRemoteHome.class, StatelessBean.class, StatelessLocal.class, StatelessLocalHome.class,
                StatelessRemote.class, StatelessRemoteHome.class);
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessLocalFromRemote(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemote bean = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemote.class.getName());
        Assert.assertNotNull(bean);

        try {
            bean.localCall();
            Assert.fail("should not be allowed to call local interface remotely");
        } catch (EJBException e) {
            // it's OK
        } catch (Exception ee) {
            if(!(ee.getCause() instanceof EJBException)) {
                Assert.fail("should be " + EJBException.class.getName());
            }
        }
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessLocalHomeFromRemoteHome(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemoteHome home = (StatefulRemoteHome) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemoteHome.class.getName());
        StatefulRemote bean = home.create();
        Assert.assertNotNull(bean);

        try {
            bean.localHomeCall();
            Assert.fail("should not be allowed to call local interface remotely");
        } catch (EJBException e) {
            // it's OK
        } catch (Exception ee) {
            if(!(ee.getCause() instanceof EJBException)) {
                Assert.fail("should be " + EJBException.class.getName());
            }
        }
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessRemoteFromRemote(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemote bean = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemote.class.getName());
        Assert.assertNotNull(bean);

        int methodCount = bean.remoteCall();
        Assert.assertEquals(1, methodCount);
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessRemoteHomeFromRemoteHome(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemoteHome home = (StatefulRemoteHome) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemoteHome.class.getName());
        StatefulRemote bean = home.create();
        Assert.assertNotNull(bean);

        int methodCount = bean.remoteHomeCall();
        Assert.assertEquals(1, methodCount);
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatefulRemoteFromRemote(@ArquillianResource InitialContext ctx) throws Exception {
        StatelessRemote bean = (StatelessRemote) ctx.lookup("java:module/" + StatelessBean.class.getSimpleName() + "!"
                + StatelessRemote.class.getName());
        Assert.assertNotNull(bean);

        int methodCount = bean.remoteCall();
        Assert.assertEquals(1, methodCount);
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatefulRemoteHomeFromRemoteHome(@ArquillianResource InitialContext ctx) throws Exception {
        StatelessRemoteHome home = (StatelessRemoteHome) ctx.lookup("java:module/" + StatelessBean.class.getSimpleName() + "!"
                + StatelessRemoteHome.class.getName());
        StatelessRemote bean = home.create();
        Assert.assertNotNull(bean);

        int methodCount = bean.remoteHomeCall();
        Assert.assertEquals(1, methodCount);
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatefulLocalFromRemote(@ArquillianResource InitialContext ctx) throws Exception {
        InitialContext jndiContext = new InitialContext();
        StatelessRemote bean = (StatelessRemote) jndiContext.lookup("java:module/" + StatelessBean.class.getSimpleName() + "!"
                + StatelessRemote.class.getName());
        Assert.assertNotNull(bean);

        try {
            bean.localCall();
            Assert.fail("should not be allowed to call local interface remotely");
        } catch (EJBException e) {
            // it's OK
        } catch (Exception ee) {
            if(!(ee.getCause() instanceof EJBException)) {
                Assert.fail("should be " + EJBException.class.getName());
            }
        }
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatefulLocalHomeFromRemoteHome(@ArquillianResource InitialContext ctx) throws Exception {
        InitialContext jndiContext = new InitialContext();
        StatelessRemoteHome home = (StatelessRemoteHome) jndiContext.lookup("java:module/"
                + StatelessBean.class.getSimpleName() + "!" + StatelessRemoteHome.class.getName());
        StatelessRemote bean = home.create();
        Assert.assertNotNull(bean);

        try {
            bean.localHomeCall();
            Assert.fail("should not be allowed to call local interface remotely");
        } catch (EJBException e) {
            // it's OK
        } catch (Exception ee) {
            if(!(ee.getCause() instanceof EJBException)) {
                Assert.fail("should be " + EJBException.class.getName());
            }
        }
    }
}