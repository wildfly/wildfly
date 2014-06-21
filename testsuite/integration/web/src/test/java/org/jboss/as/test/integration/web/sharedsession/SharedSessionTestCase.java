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
package org.jboss.as.test.integration.web.sharedsession;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SharedSessionTestCase {

    @Deployment
    public static Archive<?> dependent() {

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war")
                .addClass(SharedSessionServlet.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war")
                .addClass(SharedSessionServlet.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "sharedSession.ear")
                .addAsModule(war1)
                .addAsModule(war2)
                .addAsManifestResource(SharedSessionTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
        return ear;
    }

    @Test
    public void testSharedSessions() throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet get1 = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/war1/SharedSessionServlet");
        HttpGet get2 = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/war2/SharedSessionServlet");

        String result = runGet(get1, client);
        assertEquals("0", result);
        result = runGet(get1, client);
        assertEquals("1", result);
        result = runGet(get2, client);
        assertEquals("2", result);
        result = runGet(get2, client);
        assertEquals("3", result);
        result = runGet(get1, client);
        assertEquals("4", result);
    }

    private String runGet(HttpGet get, HttpClient client) throws IOException {
        HttpResponse res = client.execute(get);
        return EntityUtils.toString(res.getEntity());
    }
}
