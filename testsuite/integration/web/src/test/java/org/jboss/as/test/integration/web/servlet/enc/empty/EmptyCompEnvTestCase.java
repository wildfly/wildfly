/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.servlet.enc.empty;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EmptyCompEnvTestCase {

    @ArquillianResource
    @OperateOnDeployment("empty")
    private URL empty;

    @Deployment(name = "empty")
    public static WebArchive empty() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "empty.war");
        war.addClasses(HttpRequest.class, EmptyServlet.class);
        return war;
    }

    private String performCall(URL url,String urlPattern) throws Exception {
        return HttpRequest.get(url.toExternalForm() + urlPattern, 1000, SECONDS);
    }

    @Test
    @OperateOnDeployment("empty")
    public void testEmptyList() throws Exception {
        String result = performCall(empty, "simple");
        assertEquals("ok", result);
    }

}
