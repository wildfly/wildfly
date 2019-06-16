/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.multinode.clientinterceptor;

import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.controller.client.helpers.ClientConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.security.SecurityPermission;
import java.util.Arrays;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({RemoteCallClientInterceptorTestCase.SetupTask.class})
public class RemoteCallClientInterceptorTestCase {

    public static final String ARCHIVE_NAME_CLIENT = "remotelocalcall-test-client";
    public static final String ARCHIVE_NAME_SERVER = "remotelocalcall-test-server";

    private static final String moduleName = "interceptor-module";

    @Deployment(name = "server")
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment0() {
        JavaArchive jar = createJar(ARCHIVE_NAME_SERVER);
        return jar;
    }

    @Deployment(name = "client")
    @TargetsContainer("multinode-client")
    public static Archive<?> deployment1() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_CLIENT + ".jar");
        jar.addClass(StatelessRemote.class);
        jar.addClasses(RemoteCallClientInterceptorTestCase.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client-receivers.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(
                createPermissionsXmlAsset(
                        new SecurityPermission("putProviderProperty.WildFlyElytron"),createFilePermission("read,write",
                                "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery")),
                        createFilePermission("read,write",
                                "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery", "-"))),

                "permissions.xml");
        return jar;
    }

    private static JavaArchive createJar(String archiveName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, archiveName + ".jar");
        jar.addClasses(StatelessBean.class, StatelessRemote.class);
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testStateless(@ArquillianResource InitialContext ctx) throws Exception {
        StatelessRemote bean = getRemote();
        Assert.assertNotNull(bean);

        int methodCount = bean.method();
        Assert.assertEquals(1, methodCount);
        Assert.assertEquals(ClientInterceptor.invocationLatch.getCount(), 0);
        Assert.assertEquals(ClientInterceptor.resultLatch.getCount(), 0);
    }

    private StatelessRemote getRemote() throws NamingException {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new javax.naming.InitialContext(props);
        final StatelessRemote remote = (StatelessRemote) context.lookup("ejb:/" + RemoteCallClientInterceptorTestCase.ARCHIVE_NAME_SERVER + "/" + "" + "/" + "StatelessBean" + "!" + StatelessRemote.class.getName());
        return remote;
    }

    static class SetupTask implements ServerSetupTask {
        private static TestModule testModule;

        /**
         * Pack a sample interceptor to module and place to $JBOSS_HOME/modules directory
         */
        void packModule() throws Exception {
            URL url = RemoteCallClientInterceptorTestCase.class.getResource("module.xml");
            if (url == null) {
                throw new IllegalStateException("Could not find module.xml");
            }
            File moduleXmlFile = new File(url.toURI());
            testModule = new TestModule(moduleName, moduleXmlFile);
            JavaArchive jar = testModule.addResource("client-side-interceptor.jar");
            jar.addClass(ClientInterceptor.class);
            testModule.create(true);
        }

        /**
         * /subsystem=ejb3:write-attribute(name=client-interceptors,value=[{module=moduleName,class=className}])
         */
        void serverInterceptorsInfoModify(ManagementClient managementClient) throws Exception {
            final ModelNode op = new ModelNode();
            op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("client-interceptors");

            final ModelNode value = new ModelNode();
            ModelNode module = new ModelNode();
            module.get(MODULE).set(moduleName);
            module.get("class").set(ClientInterceptor.class.getName());
            value.add(module);

            op.get(VALUE).set(value);
            managementClient.getControllerClient().execute(op);
        }

        void serverInterceptorsInfoRevert(ManagementClient managementClient) throws Exception {
            final ModelNode op = new ModelNode();
            op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
            op.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("client-interceptors");

            final ModelNode operationResult = managementClient.getControllerClient().execute(op);
            // check whether the operation was successful
            assertTrue(Operations.isSuccessfulOutcome(operationResult));
        }

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            if(s.equals("multinode-client")) {
                packModule();
                serverInterceptorsInfoModify(managementClient);
                // reload in order to apply server-interceptors changes
                ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String serverName) throws Exception {
            if (serverName.equals("multinode-client")) {
                testModule.remove();
                serverInterceptorsInfoRevert(managementClient);
                // reload in order to apply server-interceptors changes
                ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
            }
        }
    }
}