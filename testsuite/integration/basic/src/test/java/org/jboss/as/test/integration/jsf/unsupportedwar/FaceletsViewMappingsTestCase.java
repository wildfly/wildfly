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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>Tests the Jakarta Server Faces deployment failure due to UnsupportedOperationException when
 * <em>jakarta.faces.FACELETS_VIEW_MAPPINGS</em> is defined with something that
 * does not include <em>*.xhtml</em>.</p>
 * <p>
 * For details check https://issues.redhat.com/browse/WFLY-13792
 *
 * @author Ranabir Chakraborty <rchakrab@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("WFLY-16521")
public class FaceletsViewMappingsTestCase {

    public static final String FACELETS_VIEW_MAPPINGS_TEST_CASE = "FaceletsViewMappingsTestCase";

    private static final String DEPLOYMENT = FaceletsViewMappingsTestCase.class.getSimpleName();

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment(name = FACELETS_VIEW_MAPPINGS_TEST_CASE, testable = false, managed = false)
    public static WebArchive deployment() {
        final Package DEPLOYMENT_PACKAGE = HelloBean.class.getPackage();
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(HelloBean.class, ConfigurationBean.class);
        war.addAsWebInfResource(DEPLOYMENT_PACKAGE, "web.xml", "web.xml");
        war.addAsWebResource(DEPLOYMENT_PACKAGE, "hello.jsf", "hello.jsf");
        return war;
    }

    @Before
    public void setup() {
        deployer.deploy(FACELETS_VIEW_MAPPINGS_TEST_CASE);
    }

    @After
    public void tearDown() {
        deployer.undeploy(FACELETS_VIEW_MAPPINGS_TEST_CASE);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testHelloWorld() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(managementClient.getWebUri() + "/" + DEPLOYMENT);

            HttpResponse response = httpclient.execute(httpget);
            String result = EntityUtils.toString(response.getEntity());

            Assert.assertEquals("Jakarta Server Faces deployment failed due to UnsupportedOperationException when jakarta.faces.FACELETS_VIEW_MAPPINGS valued wrong",
                    HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            MatcherAssert.assertThat("Hello World is in place", result, CoreMatchers.containsString("Hello World!"));
        }
    }
}
