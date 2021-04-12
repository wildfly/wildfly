/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jsf.resourceResolver;

import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
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
 * <p>Test to check that annotation FaceletsResourceResolverTest is correctly
 * managed by the JSF subsystem.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
public class FaceletsResourceResolverTestCase {

    @ArquillianResource
    URL url;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, FaceletsResourceResolverTestCase.class.getSimpleName() + ".war")
                .addClasses(CustomResourceResolver.class)
                .addAsWebResource(FaceletsResourceResolverTestCase.class.getPackage(), "index.xhtml", "index.xhtml")
                .addAsWebInfResource(
                        new StringAsset("<web-app><welcome-file-list><welcome-file>index.xhtml</welcome-file></welcome-file-list></web-app>"),
                        "web.xml");
    }

    @Test
    public void test() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpUriRequest request = new HttpGet(url.toExternalForm());
            try (CloseableHttpResponse reponse = client.execute(request)) {
                MatcherAssert.assertThat("Request success", reponse.getStatusLine().getStatusCode(), CoreMatchers.equalTo(HttpURLConnection.HTTP_OK));
                MatcherAssert.assertThat("Contains the resource-resolver message", EntityUtils.toString(reponse.getEntity()),
                        CoreMatchers.containsString("|message: " + CustomResourceResolver.class.getName() + "|"));
            }
        }
    }
}
