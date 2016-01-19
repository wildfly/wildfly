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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class SharedSessionTestCase {

    private static final String EAR_DEPLOYMENT_SHARED_SESSIONS = "sharedSession.ear";
    private static final String EAR_DEPLOYMENT_NOT_SHARED_SESSIONS = "notSharedSession.ear";

    @Deployment(name = EAR_DEPLOYMENT_SHARED_SESSIONS)
    public static Archive<?> sharedSessionEarDeployment() {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war")
                .addClass(SharedSessionServlet.class);
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war")
                .addClass(SharedSessionServlet.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_SHARED_SESSIONS)
                .addAsModule(war1)
                .addAsModule(war2)
                .addAsManifestResource(SharedSessionTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
        return ear;
    }

    @Deployment(name = EAR_DEPLOYMENT_NOT_SHARED_SESSIONS)
    public static Archive<?> notSharedEarDeployment() {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "warX.war")
                .addClass(SharedSessionServlet.class);
        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "warY.war")
                .addClass(SharedSessionServlet.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_DEPLOYMENT_NOT_SHARED_SESSIONS)
                .addAsModule(war1)
                .addAsModule(war2);
        return ear;
    }

    /**
     * Covers test case when there is EAR with enabled session sharing
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_SHARED_SESSIONS)
    public void testSharedSessionsOneEar() throws IOException {
        // Note that this test should not need to use a relaxed domain handling, however the http client does not treat ipv6 domains (e.g. ::1) properly
        HttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient();

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

        HttpClientUtils.closeQuietly(client);
    }

    private String runGet(HttpGet get, HttpClient client) throws IOException {
        HttpResponse res = client.execute(get);
        return EntityUtils.toString(res.getEntity());
    }

    /**
     * Covers test case when there is EAR with disabled session sharing
     */
    @Test
    @OperateOnDeployment(EAR_DEPLOYMENT_NOT_SHARED_SESSIONS)
    public void testNotSharedSessions() throws IOException {
        // Note that this test should not need to use a relaxed domain handling, however the http client does not treat ipv6 domains (e.g. ::1) properly
        HttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient();

        HttpGet getX = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/warX/SharedSessionServlet");
        HttpGet getY = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/warY/SharedSessionServlet");

        String result = runGet(getX, client);
        assertEquals("0", result);
        result = runGet(getX, client);
        assertEquals("1", result);
        result = runGet(getY, client);
        assertEquals("0", result);
        result = runGet(getY, client);
        assertEquals("1", result);
        result = runGet(getX, client);
        assertEquals("2", result);

        HttpClientUtils.closeQuietly(client);
    }

    /**
     * Covers test case when there is one ear with shared sessions between wars and second without sharing.
     * This test checks that the sessions sharing in one EAR doesn't intervene with sessions in second EAR
     */
    @Test
    public void testSharedSessionsDoNotInterleave() throws IOException {
        // Note that this test should not need to use a relaxed domain handling, however the http client does not treat ipv6 domains (e.g. ::1) properly
        HttpClient client = TestHttpClientUtils.promiscuousCookieHttpClient();

        HttpGet get1 = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/war1/SharedSessionServlet");
        HttpGet get2 = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/war2/SharedSessionServlet");
        HttpGet getX = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/warX/SharedSessionServlet");
        HttpGet getY = new HttpGet("http://" + TestSuiteEnvironment.getServerAddress() + ":8080/warY/SharedSessionServlet");

        String result = runGet(get1, client);
        assertEquals("0", result);
        result = runGet(get2, client);
        assertEquals("1", result);
        result = runGet(getX, client);
        assertEquals("0", result);
        result = runGet(getY, client);
        assertEquals("0", result);
        result = runGet(get1, client);
        assertEquals("2", result);

        HttpClientUtils.closeQuietly(client);
    }
}
