/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.webnontxem;

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

/**
 * test case for injecting transactional entity manager into web servlet, without a jta transaction
 *
 * @author Scott Marlow (based on Carlo's webejb test case)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class NonTransactionalEmTestCase {

    @ArquillianResource
    static URL baseUrl;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "NonTransactionalEmTestCase.war");
        war.addClasses(HttpRequest.class, SimpleServlet.class, Employee.class);
        // WEB-INF/classes is implied
        war.addAsResource(NonTransactionalEmTestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");
        war.addAsWebInfResource(NonTransactionalEmTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    private static String performCall(String urlPattern, String param) throws Exception {
        return HttpRequest.get(baseUrl.toString() + urlPattern + "?input=" + param, 10, SECONDS);
    }

    @Test
    public void testEcho() throws Exception {
        String result = performCall("simple", "Hello+world");
        assertEquals("0", result);

        result = performCall("simple", "Hello+world");
        assertEquals("0", result);
    }
}
