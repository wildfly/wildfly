/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiopssl.callerid;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * EJB that uses the {@code gssupRunAs} security domain (see {@code ssl-enable.cli}), which can
 * authenticate GSSUP (username/password) credentials and grants {@code RunAsPrincipalPermission},
 * but — unlike {@code iiopCert} — cannot verify X.509 client certificate evidence. The bean is
 * configured (see jboss-ejb3.xml) to require both GSSUP and TLS mutual authentication from the
 * client.
 *
 * When a client sends both forms of client authentication, the GSSUP credentials must be used to
 * authenticate the asserting entity, since this domain cannot authenticate the TLS client
 * certificate. This reproduces a client that relies on mutual TLS at the transport level (e.g.
 * for confidentiality/integrity) while still authenticating via GSSUP.
 */
@Stateless
@RemoteHome(CallerPropHome.class)
@SecurityDomain("gssupRunAs")
public class CallerPropGssupAndTlsBean {

    public String hello() {
        return "hello";
    }

    @PostConstruct
    public void postConstruct() {
    }
}
