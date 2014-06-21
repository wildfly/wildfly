package org.jboss.as.test.integration.web.handlers;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * Tests the use of undertow-handlers.conf
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UndertowHandlersConfigTestCase {

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "non-blocking-handler.war")
                .addPackage(UndertowHandlersConfigTestCase.class.getPackage())
                .addAsWebInfResource(UndertowHandlersConfigTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                .addAsWebResource(new StringAsset("A file"), "file.txt")
                .addAsWebInfResource(new StringAsset("regex['/rewrite.*'] -> rewrite['/file.txt']"), "undertow-handlers.conf");
    }

    @ArquillianResource
    protected URL url;

    @Test
    public void testRewrite() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "rewritea");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            Assert.assertEquals("A file", result);

            Header[] headers = response.getHeaders("MyHeader");
            Assert.assertEquals(1, headers.length);
            Assert.assertEquals("MyValue", headers[0].getValue());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }

    }
}
