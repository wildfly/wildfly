/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.manual.management;

import static org.jboss.as.controller.client.helpers.ClientConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.net.URL;
import javax.inject.Inject;

import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.WildflyTestRunner;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Tomaz Cerar
 */
@ServerControl(manual = true)
@RunWith(WildflyTestRunner.class)
public class ManagementOnlyModeTestCase {

    private static final int TEST_PORT = 20491;

    @Inject
    private ServerController container;

    @Test
    public void testManagementOnlyMode() throws Exception {
        // restart server to management-only mode
        container.startInAdminMode();

        // update the model in admin-only mode - add a web connector
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=my-test-binding", ADD);
        op.get("interface").set("public");
        op.get("port").set(TEST_PORT);

        container.getClient().executeForResult(op);

        op = createOpNode("subsystem=undertow/server=default-server/http-listener=my-test", ADD);
        op.get("socket-binding").set("my-test-binding");
        container.getClient().executeForResult(op);
        //reload to normal mode
        container.reload();

        // check that the changes made in admin-only mode have been applied - test the connector
        Assert.assertTrue("Could not connect to created connector.", WebUtil.testHttpURL(new URL(
                "http", TestSuiteEnvironment.getHttpAddress(), TEST_PORT, "/").toString()));

        // remove the conector
        op = createOpNode("subsystem=undertow/server=default-server/http-listener=my-test", REMOVE);
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(false);
        container.getClient().executeForResult(op);
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=my-test-binding", REMOVE);
        op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(false);
        ModelNode result = container.getClient().getControllerClient().execute(op);

        //reload shouldn't be required by operations above, if it is, there is a problem
        if (result.hasDefined(RESPONSE_HEADERS) && result.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE)) {
            Assert.assertTrue("reload-required".equals(result.get(RESPONSE_HEADERS).get(PROCESS_STATE).asString()));
        }
    }

}
