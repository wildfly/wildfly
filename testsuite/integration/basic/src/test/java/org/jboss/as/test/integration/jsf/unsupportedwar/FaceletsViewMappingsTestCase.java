/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.jsf.unsupportedwar;

import java.net.URL;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>Tests the Jakarta Server Faces deployment failure due to UnsupportedOperationException when
 * <em>javax.faces.FACELETS_VIEW_MAPPINGS</em> is defined with something that
 * does not include <em>*.xhtml</em>.</p>
 *
 * For details check https://issues.redhat.com/browse/WFLY-13792
 *
 * @author Ranabir Chakraborty <rchakrab@redhat.com>
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class FaceletsViewMappingsTestCase {

    private static final String DEPLOYMENT = FaceletsViewMappingsTestCase.class.getSimpleName();

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        final Package DEPLOYMENT_PACKAGE = HelloBean.class.getPackage();
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(HelloBean.class, ConfigurationBean.class);
        war.addAsWebInfResource(DEPLOYMENT_PACKAGE, "web.xml", "web.xml");
        war.addAsWebResource(DEPLOYMENT_PACKAGE, "hello.jsf", "hello.jsf");
        return war;
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testHelloWorld() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String Url = url.toExternalForm();
            HttpGet httpget = new HttpGet(Url);

            HttpResponse response = httpclient.execute(httpget);
            Assert.assertEquals("Jakarta Server Faces deployment failed due to UnsupportedOperationException when javax.faces.FACELETS_VIEW_MAPPINGS valued wrong", HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            Assert.assertThat("Hello World is in place", result, CoreMatchers.containsString("Hello World!"));
        }
    }
}
