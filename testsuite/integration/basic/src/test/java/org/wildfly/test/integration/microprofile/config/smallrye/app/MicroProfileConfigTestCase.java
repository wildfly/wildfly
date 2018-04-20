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

package org.wildfly.test.integration.microprofile.config.smallrye.app;

import static org.wildfly.test.integration.microprofile.config.smallrye.AssertUtils.assertTextContainsProperty;
import static org.wildfly.test.integration.microprofile.config.smallrye.HttpUtils.getContent;

import java.net.URL;
import java.util.Optional;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import org.wildfly.test.integration.microprofile.config.smallrye.SubsystemConfigSourceTask;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SubsystemConfigSourceTask.class)
public class MicroProfileConfigTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "MicroProfileConfigTestCase.war")
                .addClasses(TestApplication.class, TestApplication.Resource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testGetWithConfigProperties() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + "microprofile/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = getContent(response);
            assertTextContainsProperty(text, "my.prop.never.defined", Optional.empty().toString());
            assertTextContainsProperty(text, "my.prop", "BAR");
            assertTextContainsProperty(text, "my.other.prop", false);
            assertTextContainsProperty(text, "optional.injected.prop.that.is.not.configured", Optional.empty().toString());
            assertTextContainsProperty(text, SubsystemConfigSourceTask.MY_PROP_FROM_SUBSYSTEM_PROP_NAME, SubsystemConfigSourceTask.MY_PROP_FROM_SUBSYSTEM_PROP_VALUE);
        }
    }
}
