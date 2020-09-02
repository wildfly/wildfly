package org.wildfly.test.integration.microprofile.opentracing.jaxrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.wildfly.test.integration.microprofile.opentracing.jaxrs.application.Services;
import org.wildfly.test.integration.microprofile.opentracing.jaxrs.application.filters.ResponseContextProviderFilter;
import org.wildfly.test.integration.microprofile.opentracing.jaxrs.application.model.TestResponse;
import org.wildfly.test.integration.microprofile.opentracing.jaxrs.application.services.EndpointService;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class ResteasyProvidersTestCase {

    private static final HttpClient HTTP_CLIENT = HttpClients.createDefault();

    private static final String DEPLOYMENT = "deployment";
    private static final String BEANS_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<beans xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n"
            + "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "       xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd\"\n"
            + "       version=\"1.1\" bean-discovery-mode=\"all\">\n"
            + "</beans>";
    private static final String WEB_XML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<web-app xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
            + "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"\n"
            + "         metadata-complete=\"false\" version=\"3.0\">\n"
            + "    <context-param>\n"
            + "        <param-name>resteasy.providers</param-name>\n"
            + "        <param-value>\n"
            + "            org.wildfly.test.integration.microprofile.opentracing.jaxrs.application.filters.ResponseContextProviderFilter\n"
            + "        </param-value>\n"
            + "    </context-param>\n"
            + "</web-app>";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "deployment.war");
        war.addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
        war.addAsWebInfResource(new StringAsset(BEANS_XML), "beans.xml");
        war.addClasses(Services.class, ResponseContextProviderFilter.class, TestResponse.class, EndpointService.class);
        war.addAsResource(new StringAsset("Dependencies: org.apache.commons.lang3\n"), "META-INF/MANIFEST.MF");
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void doTheTest(@ArquillianResource URL url) throws Exception {
        final HttpGet get = new HttpGet(url.toExternalForm() + "test/api/ping");
        final HttpResponse response = HTTP_CLIENT.execute(get);
        Header[] testHeaders = response.getHeaders("Test-Header");
        assertNotNull(testHeaders);
        assertEquals(1, testHeaders.length);
        assertEquals("This should appear", testHeaders[0].getValue());
    }

}
