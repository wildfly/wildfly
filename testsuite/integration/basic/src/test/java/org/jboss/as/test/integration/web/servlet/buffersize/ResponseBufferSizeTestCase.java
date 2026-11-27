/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

@RunAsClient
@ExtendWith(ArquillianExtension.class)
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
    public void increaseBufferSizeTest(@ArquillianResource @OperateOnDeployment(DEPLOYMENT) URL url) throws Exception {
        URL testURL = new URL(url.toString() + "ResponseBufferSizeServlet?"
                + ResponseBufferSizeServlet.SIZE_CHANGE_PARAM_NAME + "=1.5" +
                "&" + ResponseBufferSizeServlet.DATA_LENGTH_IN_PERCENTS_PARAM_NAME + "=0.8"); // more than original size, less than new buffer size


        final HttpGet request = new HttpGet(testURL.toString());
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(request);
            Assertions.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusLine().getStatusCode(), "Failed to access " + testURL);
            String content = EntityUtils.toString(response.getEntity());
            Assertions.assertFalse(content.contains(ResponseBufferSizeServlet.RESPONSE_COMMITED_MESSAGE));
            final Header[] transferEncodingHeaders = response.getHeaders("Transfer-Encoding");
            log.trace("transferEncodingHeaders: " + Arrays.toString(transferEncodingHeaders));
            final Header[] contentLengthHeader = response.getHeaders("Content-Length");
            log.trace("contentLengthHeader: " + Arrays.toString(contentLengthHeader));

            for (Header transferEncodingHeader : transferEncodingHeaders) {
                Assertions.assertNotEquals("chunked", transferEncodingHeader.getValue(), "Transfer-Encoding shouldn't be chunked as set BufferSize shouldn't be filled yet, " +
                        "probably caused due https://bugzilla.redhat.com/show_bug.cgi?id=1212566");
            }

            Assertions.assertFalse(contentLengthHeader.length == 0, "Content-Length header not specified");
        } finally {
            IOUtils.closeQuietly(response);
            httpClient.close();
        }
    }
}
