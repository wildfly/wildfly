/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.transaction;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that java:comp/UserTransaction is bound to JNDI for web requests
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class UserTransactionBindingTest {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "tranaction.war");
        war.addPackage(UserTransactionBindingTest.class.getPackage());
        war.addClass(HttpRequest.class);
        return war;
    }


    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 10, SECONDS);
    }

    @Test
    public void testUserTransactionBound() throws Exception {
        Assertions.assertEquals("true", performCall("simple"));
    }

}
