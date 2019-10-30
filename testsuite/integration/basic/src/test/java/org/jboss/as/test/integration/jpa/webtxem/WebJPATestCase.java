/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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