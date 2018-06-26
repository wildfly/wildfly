package org.jboss.as.test.integration.web.response;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RewriteLocationByDeplomentTestCase extends ContainerResourceMgmtTestBase {

    @ArquillianResource
    Deployer deployer;

    @SuppressWarnings("WeakerAccess")
    @ContainerResource
    ManagementClient managementClient;

    @Deployment(name = "app", managed = false)
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DefaultResponseCodeTestCase.class.getSimpleName() + ".war");
        war.addClasses(SimpleRootServlet.class);
        war.addAsWebInfResource(new StringAsset("<jboss-web><context-root>/test</context-root></jboss-web>"),
                "jboss-web.xml");
        return war;
    }

    @Test
    public void testDeploymentOverLocation() throws Exception {
        try(AutoCloseable snapshot = ServerSnapshot.takeSnapshot(managementClient)) {
            // check that "/test" path returns 404
            HttpResponse response = getResponse("/test");
            Assert.assertEquals(404, response.getStatusLine().getStatusCode());

            // create location "/test" serving welcome-content
            ModelNode op = ModelUtil.createOpNode("subsystem=undertow/server=default-server/host=default-host", "add");
            op.get("address").add("location", "/test");
            op.get("handler").set("welcome-content");
            executeOperation(op);

            ServerReload.executeReloadAndWaitForCompletion(getModelControllerClient());
            response = getResponse("/test");
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            Assert.assertTrue("Expected to receive welcome page, but content length is 0",
                    response.getEntity().getContentLength() > 0);

            // deploy an app at the same location and check it's accessible
            deployer.deploy("app");
            response = getResponse("/test");
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            Assert.assertTrue("Expected to receive servlet response without any content",
                    response.getEntity().getContentLength() == 0);

            // undeploy app and check that welcome-content is accessible again
            deployer.undeploy("app");
            response = getResponse("/test");
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            Assert.assertTrue("Expected to receive welcome page, but content length is 0",
                    response.getEntity().getContentLength() > 0);
        }
    }

    private HttpResponse getResponse(String path) throws IOException {
        URI webUri = managementClient.getWebUri();
        HttpGet httpGet = new HttpGet(webUri.resolve(path));
        return HttpClientBuilder.create().build().execute(httpGet);
    }
}
