package org.jboss.as.test.integration.web.handlers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the use of undertow-handlers.conf
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ForwardedHandlerTestCase {

    private static final String FORWARDED_HANDLER_NO_UT_HANDLERS = "forwarded-handler-no-ut-handlers";
    private static final String FORWARDED_SERVLET = "forwarded-servlet";
    private static final String FORWARDED_SERVLET_NO_UT_HANDLERS = "forwarded-servlet-no-ut-handlers";

    private static final String FORWARDER_HANDLER_NAME = "forwarded";

    private static final PathAddress FORWARDER_CONF_ADDR = PathAddress.pathAddress().append(SUBSYSTEM, "undertow")
            .append("configuration", "filter").append("expression-filter", "ff");
    private static final PathAddress FORWARDER_FILTER_REF_ADDR = PathAddress.pathAddress().append(SUBSYSTEM, "undertow")
            .append("server", "default-server").append("host", "default-host").append("filter-ref", "ff");

    private static final String JBOSS_WEB_TEXT = "<?xml version=\"1.0\"?>\n" +
            "<jboss-web>\n" +
            "    <http-handler>\n" +
            "        <class-name>org.jboss.as.test.integration.web.handlers.ForwardedTestHelperHandler</class-name>\n" +
            "    </http-handler>\n" +
            "</jboss-web>";

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(name = FORWARDED_HANDLER_NO_UT_HANDLERS)
    public static WebArchive deployWithoutUndertowHandlers() {
        return ShrinkWrap.create(WebArchive.class, FORWARDED_HANDLER_NO_UT_HANDLERS + ".war")
                .addPackage(ForwardedHandlerTestCase.class.getPackage())
                .addAsWebInfResource(new StringAsset(JBOSS_WEB_TEXT), "jboss-web.xml")
                .addAsWebResource(new StringAsset("A file"), "index.html");
    }

    @Deployment(name = FORWARDED_SERVLET)
    public static WebArchive deploy_servlet() {
        return ShrinkWrap.create(WebArchive.class, FORWARDED_SERVLET + ".war")
                .addClass(ForwardedTestHelperServlet.class)
                .addAsWebInfResource(new StringAsset(FORWARDER_HANDLER_NAME), "undertow-handlers.conf")
                .addAsWebResource(new StringAsset("A file"), "index.html");
    }

    @Deployment(name = FORWARDED_SERVLET_NO_UT_HANDLERS)
    public static WebArchive deployWithoutUndertowHandlers_servlet() {
        return ShrinkWrap.create(WebArchive.class, FORWARDED_SERVLET_NO_UT_HANDLERS + ".war")
                .addClass(ForwardedTestHelperServlet.class)
                .addAsWebResource(new StringAsset("A file"), "index.html");
    }

    @Test
    @OperateOnDeployment(FORWARDED_HANDLER_NO_UT_HANDLERS)
    public void testRewriteGlobalSettings(@ArquillianResource URL url) throws Exception {
        commonConfigureExpression(url, true);
    }

    @Test
    @OperateOnDeployment(FORWARDED_SERVLET)
    public void testRewriteWithUndertowHandlersServlet(@ArquillianResource URL url) throws Exception {
        commonTestPart(new URL(url + "/forwarded"), false);
    }

    @Test
    @OperateOnDeployment(FORWARDED_SERVLET_NO_UT_HANDLERS)
    public void testRewriteGlobalSettingsServlet(@ArquillianResource URL url) throws Exception {
        commonConfigureExpression(new URL(url + "/forwarded"), false);
    }

    private void commonConfigureExpression(URL url, boolean header) throws IOException, MgmtOperationException {
        ModelNode op = Util.createAddOperation(FORWARDER_CONF_ADDR);
        op.get("expression").set(FORWARDER_HANDLER_NAME);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        op = Util.createAddOperation(FORWARDER_FILTER_REF_ADDR);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        try {
            commonTestPart(url, header);
        } finally {
            op = Util.createRemoveOperation(FORWARDER_FILTER_REF_ADDR);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
            op = Util.createRemoveOperation(FORWARDER_CONF_ADDR);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }
    }

    private void commonTestPart(URL url, boolean header) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final String proto = "https";
            final String forAddrOnly = "192.121.210.60";
            final String forAddr = forAddrOnly + ":455";
            final String byAddrOnly = "203.0.113.43";
            final String by = byAddrOnly + ":777";

            HttpGet httpget = new HttpGet(url.toExternalForm());
            httpget.addHeader("Forwarded", "for=" + forAddr + ";proto=" + proto + ";by=" + by);

            HttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            if (header) {
                Header[] hdrs = response.getHeaders(ForwardedTestHelperHandler.FORWARD_TEST_HEADER);
                Assert.assertEquals(1, hdrs.length);
                Assert.assertEquals("/" + forAddr + "|" + proto + "|" + "/" + by, hdrs[0].getValue());
            } else {
                String result = EntityUtils.toString(entity);
                Assert.assertEquals(forAddrOnly + "|" + forAddr + "|" + proto + "|" + byAddrOnly + "|" + by, result);
            }
        }
    }
}
