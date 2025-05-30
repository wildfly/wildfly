/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_class;

import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.config.smallrye.AbstractMicroProfileConfigTestCase;
import org.wildfly.test.integration.microprofile.config.smallrye.AssertUtils;

/**
 * Load a ConfigSource from a class (in a module).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SetupTask.class)
public class ConfigSourceFromClassTestCase extends AbstractMicroProfileConfigTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ConfigSourceFromClassTestCase.war")
                .addClasses(TestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(ConfigSourceFromClassTestCase.class.getPackage(), "jboss-deployment-structure.xml",
                        "jboss-deployment-structure.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testGetWithConfigProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + "custom-config-source/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = EntityUtils.toString(response.getEntity());
            AssertUtils.assertTextContainsProperty(text, CustomConfigSource.PROP_NAME, CustomConfigSource.PROP_VALUE);
            AssertUtils.assertTextContainsProperty(text, CustomConfigSource.PROP_NAME_OVERRIDEN_BY_SERVICE_LOADER,
                    CustomConfigSourceServiceLoader.PROP_VALUE_OVERRIDEN_BY_SERVICE_LOADER);
            AssertUtils.assertTextContainsProperty(text, CustomConfigSourceServiceLoader.PROP_NAME,
                    CustomConfigSourceServiceLoader.PROP_VALUE);
            // TODO - enable this when https://issues.jboss.org/browse/WFWIP-60 is resolved
            //AssertUtils.assertTextContainsProperty(text, CustomConfigSourceAServiceLoader.PROP_NAME_SAME_ORDINALITY_OVERRIDE,
            //        CustomConfigSourceAServiceLoader.PROP_VALUE_SAME_ORDINALITY_OVERRIDE);
        }
    }
}
