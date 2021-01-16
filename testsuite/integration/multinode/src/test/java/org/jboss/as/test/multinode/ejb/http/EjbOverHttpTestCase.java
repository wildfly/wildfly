/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.multinode.ejb.http;

import org.jboss.arquillian.container.test.api.Deployer;
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
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import java.net.SocketPermission;
import java.util.Arrays;
import java.util.Collections;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;


/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(EjbOverHttpTestCase.EjbOverHttpTestCaseServerSetup.class)
public class EjbOverHttpTestCase {
    private static final Logger log = Logger.getLogger(EjbOverHttpTestCase.class);
    public static final String ARCHIVE_NAME_SERVER = "ejboverhttp-test-server";
    public static final String ARCHIVE_NAME_CLIENT = "ejboverhttp-test-client";
    public static final int NO_EJB_RETURN_CODE = -1;
    private static final int serverPort = 8180;

    @ArquillianResource
    private Deployer deployer;

    static class EjbOverHttpTestCaseServerSetup implements ServerSetupTask {

        private static final PathAddress ADDR_REMOTING_PROFILE = PathAddress.pathAddress().append(SUBSYSTEM, "ejb3").append("remoting-profile", "test-profile");
        private static final PathAddress ADDR_REMOTE_HTTP_CONNECTION = ADDR_REMOTING_PROFILE.append("remote-http-connection", "remote-connection");

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);

            // /subsystem=ejb3/remoting-profile=test-profile:add()
            ModelNode remotingProfileAddModelNode = Util.createAddOperation(ADDR_REMOTING_PROFILE);
            steps.add(remotingProfileAddModelNode);

            // /subsystem=ejb3/remoting-profile=test-profile/remoting-ejb-receiver=test-connection:add(remote-http-connection=remote-ejb-connection)
            ModelNode ejbReceiverAddModelNode = Util.createAddOperation(ADDR_REMOTE_HTTP_CONNECTION);
            ejbReceiverAddModelNode.get("uri").set("http://localhost:8180/wildfly-services");
            steps.add(ejbReceiverAddModelNode);

            Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ModelNode remotingProfileRemoveModelNode = Util.createRemoveOperation(ADDR_REMOTING_PROFILE);
            //remotingProfileRemoveModelNode.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            Utils.applyUpdates(Collections.singletonList(remotingProfileRemoveModelNode), managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }
    }

    @BeforeClass
    public static void printSysProps() {
        log.trace("System properties:\n" + System.getProperties());
    }

    @Deployment(name = "server", managed = false)
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment0() {
        JavaArchive jar = createJar(ARCHIVE_NAME_SERVER);
        return jar;
    }

    @Deployment(name = "client")
    @TargetsContainer("multinode-client")
    public static Archive<?> deployment1() {
        JavaArchive clientJar = createClientJar();
        return clientJar;
    }

    private static JavaArchive createJar(String archiveName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, archiveName + ".jar");
        jar.addClasses(StatelessBean.class, StatelessLocal.class, StatelessRemote.class);
        return jar;
    }

    private static JavaArchive createClientJar() {
        JavaArchive jar = createJar(EjbOverHttpTestCase.ARCHIVE_NAME_CLIENT);
        jar.addClasses(EjbOverHttpTestCase.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client-profile.xml", "jboss-ejb-client.xml")
                .addAsManifestResource("ejb-http-wildfly-config.xml", "wildfly-config.xml")
                .addAsManifestResource(
                        createPermissionsXmlAsset(
                                createFilePermission("read,write,delete",
                                        "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery", "-")),
                                new SocketPermission(TestSuiteEnvironment.formatPossibleIpv6Address(System.getProperty("node0")) + ":" + serverPort,
                                        "connect,resolve")),
                        "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testBasicInvocation(@ArquillianResource InitialContext ctx) throws Exception {
        deployer.deploy("server");

        StatelessRemote bean = (StatelessRemote) ctx.lookup("java:module/" + StatelessBean.class.getSimpleName() + "!"
                + StatelessRemote.class.getName());
        Assert.assertNotNull(bean);

        // initial discovery
        int methodCount = bean.remoteCall();
        Assert.assertEquals(1, methodCount);

        deployer.undeploy("server");

        //  failed discovery after undeploying server deployment
        int returnValue = bean.remoteCall();
        Assert.assertEquals(NO_EJB_RETURN_CODE, returnValue);

        deployer.deploy("server");

        // rediscovery after redeployment
        methodCount = bean.remoteCall();
        Assert.assertEquals(1, methodCount);
    }
}