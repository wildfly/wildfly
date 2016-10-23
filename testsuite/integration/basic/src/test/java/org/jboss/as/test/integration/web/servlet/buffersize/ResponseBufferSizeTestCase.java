package org.jboss.as.test.integration.web.servlet.buffersize;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

@RunAsClient
@RunWith(Arquillian.class)
public class ResponseBufferSizeTestCase {

    private static final Logger log = LoggerFactory.getLogger(ResponseBufferSizeTestCase.class);

    public static final String DEPLOYMENT = "response-buffer-size.war";


    @Deployment(name = DEPLOYMENT)
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT);
        war.addClass(ResponseBufferSizeServlet.class);
        return war;
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void increaseBufferSizeTest(@ArquillianResource URL url) throws Exception {
        URL testURL = new URL(url.toString() + "ResponseBufferSizeServlet?"
                + ResponseBufferSizeServlet.SIZE_CHANGE_PARAM_NAME + "=1.5" +
                "&" + ResponseBufferSizeServlet.DATA_LENGTH_IN_PERCENTS_PARAM_NAME + "=0.8"); // more than original size, less than new buffer size


        final HttpGet request = new HttpGet(testURL.toString());
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(request);
            Assert.assertEquals("Failed to access " + testURL, HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode());
            String content = EntityUtils.toString(response.getEntity());
            Assert.assertFalse(content.contains(ResponseBufferSizeServlet.RESPONSE_COMMITED_MESSAGE));
            final Header[] transferEncodingHeaders = response.getHeaders("Transfer-Encoding");
            log.trace("transferEncodingHeaders: " + Arrays.toString(transferEncodingHeaders));
            final Header[] contentLengthHeader = response.getHeaders("Content-Length");
            log.trace("contentLengthHeader: " + Arrays.toString(contentLengthHeader));

            for (Header transferEncodingHeader : transferEncodingHeaders) {
                Assert.assertNotEquals("Transfer-Encoding shouldn't be chunked as set BufferSize shouldn't be filled yet, " +
                        "probably caused due https://bugzilla.redhat.com/show_bug.cgi?id=1212566", "chunked", transferEncodingHeader.getValue());
            }

            Assert.assertFalse("Content-Length header not specified", contentLengthHeader.length == 0);
        } finally {
            IOUtils.closeQuietly(response);
            httpClient.close();
        }
    }
}
