/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.jaxrs;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.Bravo;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;
import org.jboss.as.test.integration.jaxrs.packaging.war.WebXml;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class JaxRsCdiInterceptionTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrscdi.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addClass(JaxRsCdiInterceptionTestCase.class);
        war.add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml");
        war.addClasses(JaxRsResource.class, ResourceInterceptor.class, ComponentInterceptorBinding.class,
                ComponentInterceptor.class, Bravo.class, Alpha.class);
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" + "        <servlet-name>jakarta.ws.rs.core.Application</servlet-name>\n"
                + "        <url-pattern>/rest/*</url-pattern>\n" + "    </servlet-mapping>\n" + "\n"), "web.xml");
        return war;
    }

    @ArquillianResource
    private URL url;


    private String performCall(String urlPattern) throws Exception {
        return HttpRequest.get(url + urlPattern, 10, TimeUnit.SECONDS);
    }

    @Test
    public void testJaxRsMethodInterception() throws Exception {
        ComponentInterceptor.resetInterceptions();
        String result = performCall("rest/interception/resource");
        assertEquals("Hello World", result);

        String firstInterceptedMethod = performCall("rest/interception/resource/componentInterceptor/firstInterception");
        Assert.assertEquals("getMessage", firstInterceptedMethod);

        Boolean injectionBool = Boolean.valueOf(performCall("rest/interception/resource/injectionOk"));
        Assert.assertTrue("Jax Rs field injection not correct.", injectionBool);

        Integer intercepts = Integer.valueOf(performCall("rest/interception/resource/componentInterceptor/numberOfInterceptions"));
        Assert.assertEquals(Integer.valueOf(4), intercepts);
    }


}
