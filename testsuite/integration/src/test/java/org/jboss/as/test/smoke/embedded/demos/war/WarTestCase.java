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
package org.jboss.as.test.smoke.embedded.demos.war;

import java.net.URL;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.war.archive.SimpleServlet;
import org.jboss.as.test.smoke.modular.utils.PollingUtils;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@RunWith(Arquillian.class)
@RunAsClient
public class WarTestCase {

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return ShrinkWrapUtils.createWebArchive("demos/war-example.war", SimpleServlet.class.getPackage());
    }

    @Test
    public void testServlet() throws Exception {
        String s = performCall("simple", "Hello");
        Assert.assertEquals("Simple Servlet called with input=Hello", s);
    }

    @Test
    public void testLegacyServlet() throws Exception {
        String s = performCall("legacy", "Hello");
        Assert.assertEquals("Simple Legacy Servlet called with input=Hello", s);
    }

    private static String performCall(String urlPattern, String param) throws Exception {
        URL url = new URL("http://localhost:8080/war-example/" + urlPattern + "?input=" + param);
        PollingUtils.UrlConnectionTask task = new PollingUtils.UrlConnectionTask(url);
        PollingUtils.retryWithTimeout(10000, task);
        return task.getResponse();
    }
}
