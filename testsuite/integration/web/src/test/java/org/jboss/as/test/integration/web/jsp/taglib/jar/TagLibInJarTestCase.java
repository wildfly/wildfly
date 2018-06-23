/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.as.test.integration.web.jsp.taglib.jar;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class TagLibInJarTestCase {

    private static final String TLD_INSIDE_RESOURCES = "tld-inside-resources";
    private static final String TLD_OUTSIDE_RESOURCES = "tld-outside-resources";
    private static final String JAR_NAME = "taglib.jar";
    private static final String JSP = "index.jsp";
    private static final String WEB_FRAGMENT = "web-fragment.xml";

    @ArquillianResource
    @OperateOnDeployment(TLD_OUTSIDE_RESOURCES)
    private URL urlDep1;

    @ArquillianResource
    @OperateOnDeployment(TLD_INSIDE_RESOURCES)
    private URL urlDep2;

    @Deployment(name = TLD_OUTSIDE_RESOURCES)
    public static WebArchive deployment1() throws Exception {
        return createDeployment(TLD_OUTSIDE_RESOURCES + ".war", "tlds/taglib.tld");
    }

    @Deployment(name = TLD_INSIDE_RESOURCES)
    public static WebArchive deployment2() throws Exception {
        return createDeployment(TLD_INSIDE_RESOURCES + ".war", "resources/tlds/taglib.tld");
    }

    private static WebArchive createDeployment(String name, String tldLocation) throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME)
                .addAsManifestResource(TagLibInJarTestCase.class.getPackage(), "taglib.tld", tldLocation)
                .addAsManifestResource(TagLibInJarTestCase.class.getPackage(), WEB_FRAGMENT, WEB_FRAGMENT)
                .addClass(TestTag.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, name)
                .addAsLibraries(jar)
                .addAsWebResource(TagLibInJarTestCase.class.getPackage(), JSP, JSP);
        return war;
    }

    @Test
    @OperateOnDeployment(TLD_OUTSIDE_RESOURCES)
    public void testTldOutsideResourcesFolder() throws Exception {
        checkJspAvailable(urlDep1);
    }

    @Test
    @OperateOnDeployment(TLD_INSIDE_RESOURCES)
    public void testTldInsideResourcesFolder() throws Exception {
        checkJspAvailable(urlDep2);
    }

    private void checkJspAvailable(URL url) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String jspUrl = url.toExternalForm() + JSP;
            HttpGet httpget = new HttpGet(jspUrl);
            HttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            Assert.assertTrue(result, result.contains("Test Tag!"));
        }
    }

}
