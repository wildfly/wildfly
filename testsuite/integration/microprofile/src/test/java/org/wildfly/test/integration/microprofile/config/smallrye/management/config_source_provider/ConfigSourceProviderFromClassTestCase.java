/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source_provider;

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
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.config.smallrye.AbstractMicroProfileConfigTestCase;
import org.wildfly.test.integration.microprofile.config.smallrye.AssertUtils;
import org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.CustomConfigSource;

/**
 * Load a ConfigSource from a class (in a module).
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SetupTask.class)
public class ConfigSourceProviderFromClassTestCase extends AbstractMicroProfileConfigTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ConfigSourceProviderFromClassTestCase.war")
                .addClasses(TestApplication.class, TestApplication.Resource.class, AbstractMicroProfileConfigTestCase.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        createPermissions(CustomConfigSource.PROP_NAME)),"permissions.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testGetWithConfigProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + "custom-config-source-provider/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = EntityUtils.toString(response.getEntity());
            AssertUtils.assertTextContainsProperty(text, CustomConfigSource.PROP_NAME, CustomConfigSource.PROP_VALUE);
        }
    }
}
