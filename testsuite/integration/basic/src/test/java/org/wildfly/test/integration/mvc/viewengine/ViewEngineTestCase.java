/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc.viewengine;

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
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.CdiUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({StabilityServerSetupSnapshotRestoreTasks.Preview.class, ViewEngineTestCase.ServerSetup.class})
public class ViewEngineTestCase {

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

    public static final String SIMPLE_WAR = "ViewEngineTestCase-simple.war";
    public static final String SIMPLE_EAR = "ViewEngineTestCase-simple.ear";
    public static final String WAR_WITH_LIB = "ViewEngineTestCase-with-lib.war";
    public static final String EAR_WITH_LIB = "ViewEngineTestCase-with-lib.ear";
    private static final JavaArchive viewLib = createViewLib();

    private static JavaArchive createViewLib() {
        return ShrinkWrap.create(JavaArchive.class, "viewEngine.jar")
                .addClasses(MockViewEngine.class, MockViewEngineConfigProducer.class)
                .addAsManifestResource(CdiUtils.createBeansXml(), "beans.xml");
    }

    private static WebArchive simpleWar(String name) {
        return ShrinkWrap.create(WebArchive.class, name)
                .addClasses(TestApplication.class, TestMVCController.class);
    }

    @Deployment(name = WAR_WITH_LIB, managed = false, testable = false)
    public static WebArchive warWithLib() {
        return simpleWar(WAR_WITH_LIB)
                .addAsLibrary(viewLib);
    }

    @Deployment(name = SIMPLE_EAR, managed = false, testable = false)
    public static EnterpriseArchive simpleEar() {
        return ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR)
                .addAsModule(warWithLib());
    }

    @Deployment(name = EAR_WITH_LIB, managed = false, testable = false)
    public static EnterpriseArchive earWithLib() {
        return ShrinkWrap.create(EnterpriseArchive.class, EAR_WITH_LIB)
                .addAsModule(simpleWar(SIMPLE_WAR))
                .addAsLibrary(viewLib);
    }

    @ArquillianResource
    private Deployer deployer;

    @Test
    public void testWarLib() throws IOException {
        test(WAR_WITH_LIB, WAR_WITH_LIB);
    }

    @Test
    @Ignore("WFLY-20570")
    public void testEarLib() throws IOException {
        test(EAR_WITH_LIB, SIMPLE_WAR);
    }

    @Test
    public void testWarLibInEar() throws IOException {
        test(SIMPLE_EAR, WAR_WITH_LIB);
    }

    private void test(String deploymentName, String warName) throws IOException {
        deployer.deploy(deploymentName);
        try {
            callAndTest(warName);
        } finally {
            deployer.undeploy(deploymentName);
        }
    }

    private void callAndTest(String warName) throws IOException {
        String contextPath = warName.substring(0, warName.length() - 4);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String uri = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/" + contextPath + "/app/test";
            HttpGet get = new HttpGet(uri);
            HttpResponse response = client.execute(get);
            Assert.assertEquals("Wrong response code from " + uri, 200, response.getStatusLine().getStatusCode());
            String result = EntityUtils.toString(response.getEntity());
            Assert.assertEquals("Wrong response entity from " + uri, "Mock View", result);
        }
    }
}
