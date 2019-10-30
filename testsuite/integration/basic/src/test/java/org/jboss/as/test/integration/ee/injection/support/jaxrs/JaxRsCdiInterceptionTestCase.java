/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" + "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n"
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