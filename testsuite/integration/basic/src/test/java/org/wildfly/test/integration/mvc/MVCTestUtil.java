/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc;

import static org.jboss.as.controller.client.helpers.ClientConstants.EXTENSION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUBSYSTEM;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

public class MVCTestUtil {

    public static void callAndTest(String warName, String controllerPath, String resultContent) throws IOException {
        callAndTest(warName, "app" ,controllerPath, resultContent);
    }
    public static void callAndTest(String warName, String appPath, String controllerPath, String resultContent) throws IOException {
        String appSegment = appPath == null ? "/" : "/" + appPath + "/";
        String contextPath = warName.substring(0, warName.length() - 4);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String uri = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/" + contextPath + appSegment + controllerPath;
            HttpGet get = new HttpGet(uri);
            HttpResponse response = client.execute(get);
            Assert.assertEquals("Wrong response code from " + uri, 200, response.getStatusLine().getStatusCode());
            String result = EntityUtils.toString(response.getEntity());
            Assert.assertTrue("Wrong response entity from " + uri, result.contains(resultContent));
        }
    }

    public static final class ServerSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            executeForResult(managementClient.getControllerClient(),
                    Util.createAddOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.mvc-krazo")));
            executeForResult(managementClient.getControllerClient(),
                    Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, "mvc-krazo")));

            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            // no need to do anything as the other task uses snapshot/restore
        }

        private void executeForResult(final ModelControllerClient client, final ModelNode operation) throws Exception {
            final ModelNode response = client.execute(operation);
            Assert.assertEquals(response.toString(), SUCCESS, response.get(OUTCOME).asString());
        }
    }
}
