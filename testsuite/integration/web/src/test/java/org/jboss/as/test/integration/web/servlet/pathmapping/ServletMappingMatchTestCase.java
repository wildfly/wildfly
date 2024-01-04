/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.pathmapping;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServletMappingMatchTestCase {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-mapping.war");
        war.addPackage(ServletMappingMatchTestCase.class.getPackage());
        return war;
    }

    @Test
    public void testServlet() throws Exception {
        String s = performCall("mapping/foo", "Hello");
        Assert.assertEquals("foo:/mapping/*:PathMappingServlet", s);
    }


    private String performCall(String urlPattern, String param) throws Exception {
        URL url = new URL(this.url.toExternalForm() + urlPattern + "?input=" + param);
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}
