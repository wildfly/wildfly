/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.web.servlet.pathmapping;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServletMappingMatchTestCase {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-mapping.war");
        war.addPackage(ServletMappingMatchTestCase.class.getPackage());
        return war;
    }

    @Test
    public void testServlet() throws Exception {
        String s = performCall("mapping/foo", "Hello");
        Assert.assertEquals("foo:/mapping/*:PathMappingServlet", s);
    }


    private String performCall(String urlPattern, String param) throws Exception {
        URL url = new URL(this.url.toExternalForm() + urlPattern + "?input=" + param);
        return HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}
