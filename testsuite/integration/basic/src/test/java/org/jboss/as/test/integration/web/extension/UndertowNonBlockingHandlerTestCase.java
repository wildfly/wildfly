package org.jboss.as.test.integration.web.extension;

import java.net.URL;

import io.undertow.servlet.ServletExtension;
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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Tests the use of Undertow extensions to register a non-blocking handler
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class UndertowNonBlockingHandlerTestCase {

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "non-blocking-handler.war")
                .addPackage(UndertowNonBlockingHandlerTestCase.class.getPackage())
                .addAsServiceProvider(ServletExtension.class, SimpleUndertowExtension.class);
    }

    @ArquillianResource
    protected URL url;

    @Test
    public void testNonBlockingHandler() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet(url.toExternalForm());

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            Assert.assertEquals(SimpleUndertowExtension.THIS_IS_NOT_A_SERVLET, result);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }

    }
}
