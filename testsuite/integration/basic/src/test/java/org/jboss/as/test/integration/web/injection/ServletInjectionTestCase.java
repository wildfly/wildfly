/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.injection;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class ServletInjectionTestCase {

    @ArquillianResource
    private URL url;


    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addClasses(HttpRequest.class, SimpleServlet.class, SimpleStatelessSessionBean.class);
        return war;
    }

    private String performCall(String urlPattern, String param) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern + "?input=" + param, 10, SECONDS);
    }

    @Test
    public void testEcho() throws Exception {
        String result = performCall("simple", "Hello+world");
        assertEquals("Echo Hello world", result);
    }
}
