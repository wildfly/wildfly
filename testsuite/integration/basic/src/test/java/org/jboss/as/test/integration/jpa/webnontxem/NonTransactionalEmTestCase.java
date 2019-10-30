/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
