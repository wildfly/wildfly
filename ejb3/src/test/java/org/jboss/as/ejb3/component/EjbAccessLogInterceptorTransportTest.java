/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link EjbAccessLogInterceptor#deriveTransport(String)}.
 *
 * <p>Covers the full {@code transport} vocabulary specified in E8:
 * <ul>
 *   <li>{@code "local"}        — no-Request path (tested separately in integration; the
 *       mapping method itself is not called for the local case)</li>
 *   <li>{@code "remoting"}     — plain JBoss Remoting</li>
 *   <li>{@code "http-remoting"} — Remoting tunnelled over HTTP upgrade</li>
 *   <li>{@code "http"}         — EJB-over-HTTP; any {@code HTTP/x.y} wire version</li>
 *   <li>{@code "iiop"}         — reserved; unreachable in this release</li>
 *   <li>{@code "unknown"}      — any unrecognised value or {@code null}</li>
 * </ul>
 */
public class EjbAccessLogInterceptorTransportTest {

    // -------------------------------------------------------------------------
    // remoting
    // -------------------------------------------------------------------------

    @Test
    public void protocol_remoting_yields_remoting() {
        assertEquals("remoting", EjbAccessLogInterceptor.deriveTransport("remoting"));
    }

    // -------------------------------------------------------------------------
    // http-remoting
    // -------------------------------------------------------------------------

    @Test
    public void protocol_httpRemoting_yields_httpRemoting() {
        assertEquals("http-remoting", EjbAccessLogInterceptor.deriveTransport("http-remoting"));
    }

    // -------------------------------------------------------------------------
    // http — any HTTP/x.y wire version maps to "http"
    // -------------------------------------------------------------------------

    @Test
    public void protocol_HTTP11_yields_http() {
        assertEquals("http", EjbAccessLogInterceptor.deriveTransport("HTTP/1.1"));
    }

    @Test
    public void protocol_HTTP2_yields_http() {
        assertEquals("http", EjbAccessLogInterceptor.deriveTransport("HTTP/2.0"));
    }

    @Test
    public void protocol_HTTP3_yields_http() {
        assertEquals("http", EjbAccessLogInterceptor.deriveTransport("HTTP/3"));
    }

    // -------------------------------------------------------------------------
    // iiop — reserved
    // -------------------------------------------------------------------------

    @Test
    public void protocol_iiop_yields_iiop() {
        assertEquals("iiop", EjbAccessLogInterceptor.deriveTransport("iiop"));
    }

    // -------------------------------------------------------------------------
    // unknown — null and any unrecognised value
    // -------------------------------------------------------------------------

    @Test
    public void protocol_null_yields_unknown() {
        assertEquals("unknown", EjbAccessLogInterceptor.deriveTransport(null));
    }

    @Test
    public void protocol_unrecognised_yields_unknown() {
        assertEquals("unknown", EjbAccessLogInterceptor.deriveTransport("some-future-protocol"));
    }
}
