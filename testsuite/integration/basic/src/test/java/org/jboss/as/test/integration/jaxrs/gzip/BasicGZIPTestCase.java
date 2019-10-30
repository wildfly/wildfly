/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
                .addAsManifestResource(getProviders(), "services/javax.ws.rs.ext.Providers")
                .setWebXML(
                        WebXml.get("<servlet-mapping>\n"
                                + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
                                + "        <url-pattern>/myjaxrs/*</url-pattern>\n" + "</servlet-mapping>\n"));
    }

    private static StringAsset getProviders() {
        return new StringAsset("org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPFilter\n"
                + "org.jboss.resteasy.plugins.interceptors.encoding.GZIPDecodingInterceptor\n"
                + "org.jboss.resteasy.plugins.interceptors.encoding.GZIPEncodingInterceptor");
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
