/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.webtxem;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jpa.hibernate.entity.Company;
import org.jboss.as.test.integration.jpa.hibernate.entity.Customer;
import org.jboss.as.test.integration.jpa.hibernate.entity.Flight;
import org.jboss.as.test.integration.jpa.hibernate.entity.Ticket;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test writes and reads entity in the {@link TestServlet}. (based on the
 * EAP 5 testsuite).
 *
 * @author Zbyněk Roubalík
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebJPATestCase {

    private static final String ARCHIVE_NAME = "web_jpa";

    @ArquillianResource
    static URL baseUrl;


    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(WebJPATestCase.class, TestServlet.class,
                HttpRequest.class, Flight.class, Company.class, Customer.class,
                Ticket.class);
        // WEB-INF/classes/ is implied
        war.addAsResource(WebJPATestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");
        return war;
    }

    private static String performCall(String urlPattern, String param)
            throws Exception {
        return HttpRequest.get(baseUrl.toString() + urlPattern + "?mode=" + param, 20, SECONDS);
    }

    @Test
    public void testReadWrite() throws Exception {
        performCall("test", "write");

        String result = performCall("test", "read");
        assertEquals("Flight number one", result);
    }

}