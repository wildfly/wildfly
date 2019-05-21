/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.interceptor.serverside;

import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.jboss.as.controller.client.helpers.ClientConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ServerSetup(ServerInterceptorsTestCase.SetupTask.class)
public class ServerInterceptorsTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    private static final String moduleName = "interceptor-module";

    public static Archive<?> getArchive() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-server-interceptor.jar");
        jar.addClasses(ServerInterceptor.class, SampleBean.class, TestModule.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        return jar;
    }

    /**
     * Pack a sample interceptor to module and place to $JBOSS_HOME/modules directory
     */
    public static void doSetup() throws Exception {
        URL url = ServerInterceptorsTestCase.class.getResource("module.xml");
        if (url == null) {
            throw new IllegalStateException("Could not find module.xml");
        }
        File moduleXmlFile = new File(url.toURI());
        TestModule testModule = new TestModule(moduleName, moduleXmlFile);
        JavaArchive jar = testModule.addResource("server-side-interceptor.jar");
        jar.addClass(ServerInterceptor.class);
        testModule.create(true);
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {
        doSetup();
        return getArchive();
    }

    @Test
    public void serverInterceptorsInfoModify() throws Exception {
        // /subsystem=ejb3:write-attribute(name=server-interceptors,value=[{module=moduleName,class=className}])
        final ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("server-interceptors");

        final ModelNode value = new ModelNode();
        ModelNode module = new ModelNode();
        module.get(MODULE).set(moduleName);
        module.get("class").set(ServerInterceptor.class.getName());
        value.add(module);

        op.get(VALUE).set(value);
        final ModelNode writeAttributeOperationResult = managementClient.getControllerClient().execute(op);

        assertEquals(SUCCESS, writeAttributeOperationResult.get(OUTCOME).asString());
    }

    @Test
    public void serverInterceptorsInfoRetrieve() throws Exception {
        // /subsystem=ejb3:read-attribute(name=server-interceptors)
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(SUBSYSTEM, "ejb3");
        op.get(NAME).set("server-interceptors");

        final ModelNode operationResult = managementClient.getControllerClient().execute(op);
        assertTrue(operationResult.get(RESULT).asString().contains(moduleName));
    }

    static class SetupTask implements ServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            // empty
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            // reload in order to apply server-interceptors changes
            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }
    }
}
