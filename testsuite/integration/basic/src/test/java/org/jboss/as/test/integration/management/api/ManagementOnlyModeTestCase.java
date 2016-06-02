/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.api;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Tomaz Cerar
 */
@RunWith(Arquillian.class)
@RunAsClient
//todo this should be moved to manual mode tests and use WildFly runner instead
public class ManagementOnlyModeTestCase extends ContainerResourceMgmtTestBase {

    @ArquillianResource
    URL url;
    private static final int TEST_PORT = 9091;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(ManagementOnlyModeTestCase.class);
        return ja;
    }

    @Test
    public void testManagementOnlyMode() throws Exception {
        // restart server to management-only mode
        ServerReload.executeReloadAndWaitForCompletion(getModelControllerClient(), true);//todo with using WildFlyRunner we could start in admin mode from the beginning

        // check that the server is unreachable
        Assert.assertFalse("Could not connect to created connector.", WebUtil.testHttpURL(new URL(
                "http", url.getHost(), url.getPort(), "/").toString()));

        // update the model in admin-only mode - add a web connector
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=my-test-binding", ADD);
        op.get("interface").set("public");
        op.get("port").set(TEST_PORT);
        ModelNode result = executeOperation(op);

        op = createOpNode("subsystem=undertow/server=default-server/http-listener=my-test", ADD);
        op.get("socket-binding").set("my-test-binding");
        result = executeOperation(op);
        //reload to normal mode
        ServerReload.executeReloadAndWaitForCompletion(getModelControllerClient(), false);

        // check that the server is up
        Assert.assertTrue("Could not connect to created connector.", WebUtil.testHttpURL(new URL(
                "http", url.getHost(), url.getPort(), "/").toString()));

        // check that the changes made in admin-only mode have been applied - test the connector
        Assert.assertTrue("Could not connect to created connector.", WebUtil.testHttpURL(new URL(
                "http", url.getHost(), TEST_PORT, "/").toString()));

        // remove the conector
        op = createOpNode("subsystem=undertow/server=default-server/http-listener=my-test", REMOVE);
        //op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = executeOperation(op);
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=my-test-binding", REMOVE);
        //op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        result = executeOperation(op);

        ServerReload.executeReloadAndWaitForCompletion(getModelControllerClient());//todo this is completely wrong
    }

}
