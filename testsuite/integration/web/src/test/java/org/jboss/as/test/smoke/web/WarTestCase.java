/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.web;

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
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WarTestCase {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addPackage(WarTestCase.class.getPackage());
        war.setWebXML(WarTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void testServlet() throws Exception {
        String s = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", s);
    }

    @Test
    public void testLegacyServlet() throws Exception {
        String s = performCall("legacy", "Hello");
        Assert.assertEquals("Simple Legacy Servlet called with input=Hello", s);
    }

    private String performCall(String urlPattern, String param) throws Exception {
        URL url = new URL(this.url.toExternalForm() + urlPattern + "?input=" + param);
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}
