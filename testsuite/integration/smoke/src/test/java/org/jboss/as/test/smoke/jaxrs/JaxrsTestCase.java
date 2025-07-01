/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.jaxrs;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class JaxrsTestCase {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment(){
        final WebArchive war = create(WebArchive.class, "jaxrs-example.war");
        war.addPackage(JaxrsTestCase.class.getPackage());
        war.setWebXML(JaxrsTestCase.class.getPackage(), "web.xml");
        return war;
    }

    @Test
    public void testJaxrs() throws Exception {
        String s = performCall();
        Assertions.assertEquals("Hello World!", s);
    }

    private String performCall() throws Exception {
        URL url = new URL(this.url.toExternalForm() + "helloworld");
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}
