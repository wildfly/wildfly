/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.enc.empty;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EmptyCompEnvTestCase {

    @ArquillianResource
    @OperateOnDeployment("empty")
    private URL empty;

    @Deployment(name = "empty")
    public static WebArchive empty() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "empty.war");
        war.addClasses(HttpRequest.class, EmptyServlet.class);
        return war;
    }

    private String performCall(URL url,String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 1000, SECONDS);
    }

    @Test
    @OperateOnDeployment("empty")
    public void testEmptyList() throws Exception {
        String result = performCall(empty, "simple");
        assertEquals("ok", result);
    }

}
