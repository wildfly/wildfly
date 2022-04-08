/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir;

import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.SetupTask.A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.SetupTask.B;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.B_OVERRIDES_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.DEFAULT;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.FROM_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.FROM_B;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.NOT_AVAILABLE_NESTED_DIR_UNDER_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir.TestApplication.NOT_AVAILABLE_NESTED_DIR_UNDER_B;

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
public class ConfigSourceFromDirTestCase extends AbstractMicroProfileConfigTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ConfigSourceFromDirTestCase.war")
                .addClasses(TestApplication.class, TestApplication.Resource.class, AbstractMicroProfileConfigTestCase.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
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
            AssertUtils.assertTextContainsProperty(text, FROM_A, A);
            AssertUtils.assertTextContainsProperty(text, FROM_B, B);
            AssertUtils.assertTextContainsProperty(text, B_OVERRIDES_A, SetupTask.OVERRIDDEN_B);
            AssertUtils.assertTextContainsProperty(text, NOT_AVAILABLE_NESTED_DIR_UNDER_A, DEFAULT);
            AssertUtils.assertTextContainsProperty(text, NOT_AVAILABLE_NESTED_DIR_UNDER_B, DEFAULT);
        }
    }
}
