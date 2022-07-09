package org.jboss.as.test.integration.web.response;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletResponse;
import java.net.URL;

/**
 * Tests the "default servlet" of the web container
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DefaultServletTestCase {

    private static final String WEB_APP_CONTEXT = "default-servlet-test";
    private static final String APP_XHTML_FILE_NAME = "app.xhtml";
    private static final String INFDIRS_DEPLOYMENT = "infdirectories";
    private static final Logger logger = Logger.getLogger(DefaultServletTestCase.class);

    @ArquillianResource
    URL url;

    private HttpClient httpclient;

    @Deployment(name = WEB_APP_CONTEXT)
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_APP_CONTEXT + ".war");
        war.addAsWebResource(DefaultServletTestCase.class.getPackage(), APP_XHTML_FILE_NAME, APP_XHTML_FILE_NAME);
        return war;
    }

    @Deployment(name = INFDIRS_DEPLOYMENT)
    public static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, INFDIRS_DEPLOYMENT + ".war");
        war.add(new StringAsset("Welcome in WEB-INFOOBAR"), "WEB-INFOOBAR/test.html");
        war.add(new StringAsset("Welcome in META-INFOOBAR"), "META-INFOOBAR/test.html");
        return war;
    }

    @Before
    public void setup() {
        this.httpclient = HttpClientBuilder.create().build();
    }

    /**
     * Tests that the default servlet doesn't show the source (code) of a resource when an incorrect URL is used to access that resource.
     *
     * @throws Exception
     * @see https://developer.jboss.org/thread/266805 for more details
     */
    @OperateOnDeployment(WEB_APP_CONTEXT)
    @Test
    public void testForbidSourceFileAccess() throws Exception {
        // first try accessing the valid URL and expect it to serve the right content
        final String correctURL = url.toString() + APP_XHTML_FILE_NAME;
        final HttpGet httpGetCorrectURL = new HttpGet(correctURL);
        final HttpResponse response = this.httpclient.execute(httpGetCorrectURL);
        Assert.assertEquals("Unexpected response code for URL " + correctURL, HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
        final String content = EntityUtils.toString(response.getEntity());
        Assert.assertTrue("Unexpected content served at " + correctURL, content.contains("Hello World"));

        // now try accessing the same URL with a "." at the end of the resource name.
        // This should throw a 404 error and NOT show up the "source" content of the resource
        final String nonExistentURL = url.toString() + APP_XHTML_FILE_NAME + ".";
        final HttpGet httpGetNonExistentURL = new HttpGet(nonExistentURL);
        final HttpResponse responseForNonExistentURL = this.httpclient.execute(httpGetNonExistentURL);
        Assert.assertEquals("Unexpected response code for URL " + nonExistentURL, HttpServletResponse.SC_NOT_FOUND, responseForNonExistentURL.getStatusLine().getStatusCode());
    }

    /**
     * Tests if the default servlet serves content from any directories starting with WEB-INF or META-INF
     *
     * [WFLY-15045]
     *
     * @param webAppURL
     * @throws Exception
     */
    @OperateOnDeployment(INFDIRS_DEPLOYMENT)
    @Test
    public void testInfDirectories(@ArquillianResource URL webAppURL) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpResponse httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "WEB-INFOOBAR/test.html"));
        Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
        EntityUtils.consumeQuietly(httpResponse.getEntity());
        httpResponse = httpClient.execute(new HttpGet(webAppURL.toURI() + "META-INFOOBAR/test.html"));
        Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());
    }
}
