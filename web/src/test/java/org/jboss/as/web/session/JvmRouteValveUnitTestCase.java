/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.apache.catalina.Session;
import org.apache.catalina.connector.Response;
import org.jboss.as.web.session.mocks.MockClusteredSessionManager;
import org.jboss.as.web.session.mocks.MockRequest;
import org.jboss.as.web.session.mocks.MockValve;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * Tests of the JvmRouteValve.
 *
 * @author Brian Stansberry
 */
public class JvmRouteValveUnitTestCase {
    private static final Logger log = Logger.getLogger(JvmRouteValveUnitTestCase.class);

    private static final String JVM_ROUTE = "node1";
    private static final String NON_FAILOVER_ID = "123." + JVM_ROUTE;
    private static final String FAILOVER_ID = "123.node2";
    private static final String NO_JVMROUTE_ID = "123";

    private static final String DOMAIN_JVM_ROUTE = "domain1.node1";
    private static final String DOMAIN_NON_FAILOVER_ID = "123." + DOMAIN_JVM_ROUTE;
    private static final String DOMAIN_FAILOVER_ID = "123.domain1.node2";

    @Test
    public void testNonFailover() throws Exception {
        log.info("Enter testNonFailover");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(NON_FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(session.getId());

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testFailover() throws Exception {
        log.info("Enter testFailover");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(session.getId());

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(NON_FAILOVER_ID, mgr.getNewCookieIdSession());

    }

    @Test
    public void testFailoverFromURL() throws Exception {
        log.info("Enter testFailoverFromURL");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(session.getId());
        req.setRequestedSessionIdFromURL(true);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testFailoverMismatchBadReq() throws Exception {
        log.info("Enter testFailoverMismatchBadReq");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(NON_FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(FAILOVER_ID);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(NON_FAILOVER_ID, mgr.getNewCookieIdSession());
    }

    @Test
    public void testFailoverMismatchBadReqFromURL() throws Exception {
        log.info("Enter testFailoverMismatchBadReqFromURL");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(NON_FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(FAILOVER_ID);
        req.setRequestedSessionIdFromURL(true);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testFailoverMismatchBadSession() throws Exception {
        log.info("Enter testFailoverMismatchBadSession");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(NON_FAILOVER_ID);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(NON_FAILOVER_ID, mgr.getNewCookieIdSession());
    }

    @Test
    public void testFailoverMismatchBadSessionFromURL() throws Exception {
        log.info("Enter testFailoverMismatchBadSessionFromURL");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(NON_FAILOVER_ID);
        req.setRequestedSessionIdFromURL(true);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testNoSession() throws Exception {
        log.info("Enter testNoSession");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        req.setRequestedSessionId(NON_FAILOVER_ID);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(null, mgr.findSession("123.node1"));
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testNoSessionFromURL() throws Exception {
        log.info("Enter testNoSessionFromURL");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        req.setRequestedSessionId(NON_FAILOVER_ID);
        req.setRequestedSessionIdFromURL(true);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(null, mgr.findSession(NON_FAILOVER_ID));
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testFailoverNoSession() throws Exception {
        log.info("Enter testFailoverNoSession");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        req.setRequestedSessionId(FAILOVER_ID);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(null, mgr.findSession(NON_FAILOVER_ID));
        assertEquals(null, mgr.findSession("123.node2"));
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testNoSessionNoRequestedSession() throws Exception {
        log.info("Enter testNoSessionNoRequestedSession");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(null, mgr.findSession(NON_FAILOVER_ID));
        assertEquals(null, mgr.findSession(FAILOVER_ID));
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testSessionNoRequestedSession() throws Exception {
        log.info("Enter testSessionNoRequestedSession");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        Session session = mgr.createSession(NON_FAILOVER_ID);
        MockRequest req = new MockRequest(mgr);
        req.setSession(session);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(NON_FAILOVER_ID, mgr.getNewCookieIdSession());
    }

    @Test
    public void testSessionNoRequestedSessionFromURL() throws Exception {
        log.info("Enter testSessionNoRequestedSessionFromURL");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        Session session = mgr.createSession(NON_FAILOVER_ID);
        MockRequest req = new MockRequest(mgr);
        req.setSession(session);
        req.setRequestedSessionIdFromURL(true);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testFailoverSessionNoRequestedSession() throws Exception {
        log.info("Enter testFailoverSessionNoRequestedSession");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        Session session = mgr.createSession(FAILOVER_ID);
        MockRequest req = new MockRequest(mgr);
        req.setSession(session);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(NON_FAILOVER_ID, mgr.getNewCookieIdSession());
    }

    @Test
    public void testFailoverSessionNoRequestedSessionFromURL() throws Exception {
        log.info("Enter testFailoverSessionNoRequestedSessionFromURL");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        Session session = mgr.createSession(FAILOVER_ID);
        MockRequest req = new MockRequest(mgr);
        req.setSession(session);
        req.setRequestedSessionIdFromURL(true);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testNoJvmRouteSession() throws Exception {
        log.info("Enter testNoJvmRouteSession");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        Session session = mgr.createSession(NO_JVMROUTE_ID);
        MockRequest req = new MockRequest(mgr);
        req.setSession(session);
        req.setRequestedSessionId(session.getId());

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(NON_FAILOVER_ID, mgr.getNewCookieIdSession());
    }

    @Test
    public void testNoJvmRouteSessionFromURL() throws Exception {
        log.info("Enter testNoJvmRouteSessionFromURL");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        Session session = mgr.createSession(NO_JVMROUTE_ID);
        MockRequest req = new MockRequest(mgr);
        req.setSession(session);
        req.setRequestedSessionId(session.getId());
        req.setRequestedSessionIdFromURL(true);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testNoJvmRouteRequest() throws Exception {
        log.info("Enter testNoJvmRouteRequest");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        Session session = mgr.createSession(NON_FAILOVER_ID);
        MockRequest req = new MockRequest(mgr);
        req.setSession(session);
        req.setRequestedSessionId(NO_JVMROUTE_ID);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(NON_FAILOVER_ID, mgr.getNewCookieIdSession());
    }

    @Test
    public void testNoJvmRouteRequestFromURL() throws Exception {
        log.info("Enter testNoJvmRouteRequest");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(NON_FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(NO_JVMROUTE_ID);
        req.setRequestedSessionIdFromURL(true);

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(NON_FAILOVER_ID, session.getId());
        assertEquals(null, mgr.getNewCookieIdSession());
    }

    @Test
    public void testJvmRouteIncludesDomain() throws Exception {
        log.info("Enter testJvmRouteIncludesDomain");

        MockClusteredSessionManager mgr = new MockClusteredSessionManager();
        mgr.setJvmRoute(DOMAIN_JVM_ROUTE);

        JvmRouteValve jvmRouteValve = new JvmRouteValve(mgr);

        MockValve mockValve = new MockValve();

        jvmRouteValve.setNext(mockValve);

        MockRequest req = new MockRequest(mgr);
        Session session = mgr.createSession(DOMAIN_FAILOVER_ID);
        req.setSession(session);
        req.setRequestedSessionId(session.getId());

        Response res = new Response();

        jvmRouteValve.invoke(req, res);

        assertSame(req, mockValve.getInvokedRequest());
        assertSame(res, mockValve.getInvokedResponse());
        assertEquals(DOMAIN_NON_FAILOVER_ID, session.getId());
        assertEquals(DOMAIN_NON_FAILOVER_ID, mgr.getNewCookieIdSession());

    }
}
