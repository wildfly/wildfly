/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.http;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.util.HttpMgmtProxy;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests all management operation types which are available via HTTP GET requests.
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class HttpMgmtPrettyTestCase {

    private static final int MGMT_PORT = 9990;
    private static final String MGMT_CTX = "/management";
    @ArquillianResource
    URL url;
    private HttpMgmtProxy httpMgmt;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(HttpMgmtPrettyTestCase.class);
        return ja;
    }

    @Before
    public void before() throws Exception {
        URL mgmtURL = new URL(url.getProtocol(), url.getHost(), MGMT_PORT, MGMT_CTX);
        httpMgmt = new HttpMgmtProxy(mgmtURL);
    }

    @Test
    public void testPrettyDefaultNo() throws Exception {
        checkServerJson(null, false);
    }

    @Test
    public void testPrettyFalse() throws Exception {
        checkServerJson("json.pretty=false", false);
    }

    @Test
    public void testPrettyZero() throws Exception {
        checkServerJson("json.pretty=0", false);
    }

    @Test
    public void testPrettyTrue() throws Exception {
        checkServerJson("json.pretty=true", true);
    }

    @Test
    public void testPrettyOne() throws Exception {
        checkServerJson("json.pretty=1", true);
    }

    private void checkServerJson(String queryString, boolean pretty) throws Exception {
        final String query = queryString == null ? "null" : "?" + queryString;
        String reply = httpMgmt.sendGetCommandJson(query);
        if (pretty) {
            Assert.assertTrue(reply.contains("\n"));
        } else {
            Assert.assertFalse(reply.contains("\n"));
        }
    }
}
