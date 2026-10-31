/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests the RESTEasy exception handling
 *
 * @author Pavel Janousek
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class ExceptionHandlingTestCase {

    @Deployment
    public static Archive<?> deploy_true() {
        return ShrinkWrap
                .create(WebArchive.class, "exception.war")
                .addClasses(ExceptionHandlingTestCase.class, HelloWorldResource.class, NPExceptionMapper.class)
                .setWebXML(
                        WebXml.get("<servlet-mapping>\n"
                                + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
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
            Assertions.fail("Should not go there - NullPointerException must occurred!");
        } catch (Exception e) {
            Assertions.assertTrue(e.toString().contains("HTTP Status 404"));
        }
    }

    @Test
    public void testArrayIndexOutOfBoundsException(@ArquillianResource URL url) throws Exception {
        try {
            @SuppressWarnings("unused")
            String result = HttpRequest.get(url.toExternalForm() + "myjaxrs/helloworld/ex2", 10, TimeUnit.SECONDS);
            Assertions.fail("Should not go there - ArrayIndexOutOfBoundsException must occurred!");
        } catch (Exception e) {
            Assertions.assertFalse(e.toString().contains("HTTP Status 404"));
            Assertions.assertTrue(e.toString().contains("HTTP Status 500"));
        }
    }
}
