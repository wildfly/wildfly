/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.smoke.embedded.demos.jaxrs;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.jaxrs.archive.HelloWorldResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsTestCase {

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jaxrs-example.war");
        war.addPackage(HttpRequest.class.getPackage());
        war.addClasses(JaxrsTestCase.class, HelloWorldResource.class);
        war.addAsWebInfResource(WebXml.get("<servlet-mapping>\n" +
                "        <servlet-name>javax.ws.rs.core.Application</servlet-name>\n" +
                "        <url-pattern>/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                "\n"), "web.xml");
        return war;

    }

    @Test
    public void testJaxrs() throws Exception {
        String s = performCall();
        Assert.assertEquals("Hello World!", s);
    }

    private static String performCall() throws Exception {
        URL url = new URL("http://localhost:8080/jaxrs-example/helloworld");
        PollingUtils.UrlConnectionTask task = new PollingUtils.UrlConnectionTask(url);
        PollingUtils.retryWithTimeout(10000, task);
        return task.getResponse();
    }
}
