/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.enventry;

import java.util.concurrent.TimeUnit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

/**
 * A test for injection via env-entry in web.xml
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EnvEntryInjectionServletTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addClasses(EnvEntryInjectionServlet.class);
        war.addAsWebInfResource(EnvEntryInjectionServletTestCase.class.getPackage(), "EnvEntryInjectionServletTestCase-web.xml", "web.xml");
        return war;
    }

    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(managementClient.getWebUri() + "/war-example/" + urlPattern, 5, TimeUnit.SECONDS);
    }

    @Test
    public void testEnvEntryInjectionIntoServlet() throws Exception {
        String result = performCall("envEntry");
        assertEquals("injection!", result);
    }
}
