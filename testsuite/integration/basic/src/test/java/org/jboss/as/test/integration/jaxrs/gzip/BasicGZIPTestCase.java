/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.gzip;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for RESTEasy @GZIP annotation feature
 *
 * @author Pavel Janousek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BasicGZIPTestCase {

    private static final Properties gzipProp = new Properties();

    {
        gzipProp.setProperty("Accept-Encoding", "gzip,deflate");
    }

    @Deployment
    public static Archive<?> deploy_true() {
        return ShrinkWrap
                .create(WebArchive.class, "gzip.war")
                .addClasses(BasicGZIPTestCase.class, GZIPResource.class, JaxbModel.class)
                .setWebXML(
                        WebXml.get("<servlet-mapping>\n"
                                + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
                                + "        <url-pattern>/myjaxrs/*</url-pattern>\n" + "</servlet-mapping>\n"
                                + " <context-param><param-name>resteasy.allowGzip</param-name><param-value>true</param-value></context-param>\n"));
    }

    private String read(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toString();
    }

    private String getGzipResult(final String url) throws Exception {
        final HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
        conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
        conn.setDoInput(true);
        InputStream in = conn.getInputStream();
        Assert.assertTrue(conn.getContentEncoding().contains("gzip"));
        in = new GZIPInputStream(in);
        String result = read(in);
        in.close();
        Assert.assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
        return result;
    }

    @Test
    public void testPlainString(@ArquillianResource URL url) throws Exception {
        final String res_string = "Hello World!";

        String result = HttpRequest.get(url.toExternalForm() + "myjaxrs/helloworld", 10, TimeUnit.SECONDS);
        assertEquals(res_string, result);

        result = getGzipResult(url.toExternalForm() + "myjaxrs/helloworld");
        assertEquals(res_string, result);
    }

    @Test
    public void testXml(@ArquillianResource URL url) throws Exception {
        final String res_string = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><jaxbModel><first>John</first><last>Citizen</last></jaxbModel>";

        String result = HttpRequest.get(url.toExternalForm() + "myjaxrs/helloworld/xml", 10, TimeUnit.SECONDS);
        assertEquals(res_string, result);

        result = getGzipResult(url.toExternalForm() + "myjaxrs/helloworld/xml");
        assertEquals(res_string, result);
    }

}
