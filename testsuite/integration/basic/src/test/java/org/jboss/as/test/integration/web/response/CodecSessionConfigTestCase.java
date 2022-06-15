/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.test.integration.web.response;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

/**
 * CodecSessionConfigTestCase
 * For more information visit <a href="https://issues.redhat.com/browse/WFLY-10912">https://issues.redhat.com/browse/WFLY-10912</a>
 * @author Petr Adamec
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CodecSessionConfigTestCase {
    private static final String URL_PATTERN = "/";
    @ArquillianResource
    private URL url;
    private HttpClient httpclient = null;

    @Before
    public void setup() {
        this.httpclient = HttpClientBuilder.create().build();
    }

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, CodecSessionConfigTestCase.class.getSimpleName() + ".war");
        war.addAsWebResource(new StringAsset(CodecSessionConfigTestCase.class.getSimpleName()), "index.html");
        return war;
    }

    /**
     * The issue happens when the client sends JSESSIONID Cookie in the request to the web application does NOT use HttpSession.
     * JSESSIONID Set-Cookie response header should not be sent in this scenario, but WildFly/EAP 7 returns the response with JSESSIONID
     * reusing the requested session id which does not exist in the session manager.
     * <br /> For more information see https://issues.redhat.com/browse/WFLY-10912.
     * @throws Exception
     */
    @Test
    public void testResponseSessionId() throws Exception {
        HttpGet httpget = null;
        HttpResponse response = null;
        String stringUrl = url.toString() + URL_PATTERN+"index.html";
        httpget = new HttpGet(stringUrl);
        httpget.addHeader("Cookie", "JSESSIONID=foobar");
        response = this.httpclient.execute(httpget);
        Header[] headers = response.getHeaders("Set-Cookie");
        if(headers.length > 0) {
            Assert.fail("The expected behavior is that WildFly/EAP 7 should respond without JSESSIONID Set-Cookie header. See https://issues.redhat.com/browse/WFLY-10912");
        }
    }
}
