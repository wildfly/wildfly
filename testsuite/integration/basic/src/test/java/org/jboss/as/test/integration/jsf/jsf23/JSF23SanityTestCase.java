/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.jsf.jsf23;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A simple test to verify that JSF 2.3 is used.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JSF23SanityTestCase {
    @ArquillianResource
    private URL url;

    private final Pattern viewStatePattern = Pattern.compile("id=\".*javax.faces.ViewState.*\" value=\"([^\"]*)\"");

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "registration-jsf.war");
        war.addClass(ConfirmBean.class);
        war.addClass(ConfirmRegistrationBehavior.class);
        war.addClass(DummyBean.class);
        war.addAsWebResource(JSF23SanityTestCase.class.getPackage(), "index.xhtml", "index.xhtml");
        war.addAsWebResource(JSF23SanityTestCase.class.getPackage(), "confirmation.xhtml", "confirmation.xhtml");
        war.addAsWebInfResource(JSF23SanityTestCase.class.getPackage(), "beans.xml", "beans.xml");
        war.addAsWebInfResource(JSF23SanityTestCase.class.getPackage(), "register.taglib.xml", "register.taglib.xml");
        war.addAsWebInfResource(JSF23SanityTestCase.class.getPackage(), "faces-config.xml", "faces-config.xml");
        war.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n"), "MANIFEST.MF");
        return war;
    }

    @Test
    public void testJSF23InjectCanBeUsed() throws Exception {
        String responseString;
        DefaultHttpClient client = new DefaultHttpClient();

        try {
            // Create and execute a GET request
            String jsfViewState = null;
            String requestUrl = url.toString() + "index.jsf";
            HttpGet getRequest = new HttpGet(requestUrl);
            HttpResponse response = client.execute(getRequest);
            try {
                responseString = IOUtils.toString(response.getEntity().getContent(), "UTF-8");

                // Get the JSF view state
                Matcher jsfViewMatcher = viewStatePattern.matcher(responseString);
                if (jsfViewMatcher.find()) {
                    jsfViewState = jsfViewMatcher.group(1);
                }
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // Create and execute a POST request
            HttpPost post = new HttpPost(requestUrl);

            List<NameValuePair> list = new ArrayList<NameValuePair>();
            list.add(new BasicNameValuePair("javax.faces.ViewState", jsfViewState));
            list.add(new BasicNameValuePair("register", "register"));
            list.add(new BasicNameValuePair("register:registerButton", "Register"));

            post.setEntity(new StringEntity(URLEncodedUtils.format(list, "UTF-8"), ContentType.APPLICATION_FORM_URLENCODED));
            response = client.execute(post);

            try {
                responseString = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
        assertTrue(responseString.contains("JSF 2.3 Inject worked!"));
    }
}
