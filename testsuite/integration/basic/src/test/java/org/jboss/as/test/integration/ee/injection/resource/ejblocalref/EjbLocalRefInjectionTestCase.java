/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.ejblocalref;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A test for injection via env-entry in web.xml
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class EjbLocalRefInjectionTestCase {

    @ArquillianResource
    private ManagementClient managementClient;


    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addClasses(EjbLocalRefInjectionServlet.class, NamedSLSB.class, SimpleSLSB.class, Hello.class);
        war.addAsWebInfResource(EjbLocalRefInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(managementClient.getWebUri() + "/war-example/" + urlPattern, 5, TimeUnit.SECONDS);
    }

    @Test
    public void testLookup() throws Exception {
        String result = performCall("ejbLocalRef?type=simple");
        assertEquals("Simple Hello", result);
    }

    @Test
    public void testEjbLink() throws Exception {
        String result = performCall("ejbLocalRef?type=named");
        assertEquals("Named Hello", result);
    }
}
