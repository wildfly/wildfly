/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.registration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class DefaultServletReplacmentTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive single() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "single.war");
        war.addClasses(HttpRequest.class, ReplacementServlet.class, DefaultReplacingServletContextListener.class);
        return war;
    }

    private String performCall(URL url, String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 1000, SECONDS);
    }


    @Test
    public void testLifeCycle() throws Exception {
        String result = performCall(url, "/");
        assertEquals("ok", result);
    }
}
