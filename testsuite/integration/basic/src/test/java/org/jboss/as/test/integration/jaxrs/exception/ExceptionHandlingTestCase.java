/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jaxrs.exception;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the RESTEasy exception handling
 *
 * @author Pavel Janousek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ExceptionHandlingTestCase {

    @Deployment
    public static Archive<?> deploy_true() {
        return ShrinkWrap
                .create(WebArchive.class, "exception.war")
                .addClasses(ExceptionHandlingTestCase.class, HelloWorldResource.class, NPExceptionMapper.class)
                .setWebXML(
                        WebXml.get("<servlet-mapping>\n"
                                + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
                                + "        <url-pattern>/myjaxrs/*</url-pattern>\n" + "</servlet-mapping>\n"));
    }

    @Test
    public void testResource(@ArquillianResource URL url) throws Exception {
        String result = HttpRequest.get(url.toExternalForm() + "myjaxrs/helloworld", 10, TimeUnit.SECONDS);
        assertEquals("Hello World!", result);
    }

    @Test
    public void testNullPointerException(@ArquillianResource URL url) throws Exception {
        try {
            @SuppressWarnings("unused")
            String result = HttpRequest.get(url.toExternalForm() + "myjaxrs/helloworld/ex1", 10, TimeUnit.SECONDS);
            Assert.fail("Should not go there - NullPointerException must occurred!");
        } catch (Exception e) {
            Assert.assertTrue(e.toString().contains("HTTP Status 404"));
        }
    }

    @Test
    public void testArrayIndexOutOfBoundsException(@ArquillianResource URL url) throws Exception {
        try {
            @SuppressWarnings("unused")
            String result = HttpRequest.get(url.toExternalForm() + "myjaxrs/helloworld/ex2", 10, TimeUnit.SECONDS);
            Assert.fail("Should not go there - ArrayIndexOutOfBoundsException must occurred!");
        } catch (Exception e) {
            Assert.assertFalse(e.toString().contains("HTTP Status 404"));
            Assert.assertTrue(e.toString().contains("HTTP Status 500"));
        }
    }
}
