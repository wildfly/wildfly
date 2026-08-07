/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiopssl.callerid;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;

/**
 * EJB that uses the default security domain ("other" / ApplicationDomain)
 * instead of the X.509-capable "iiopCert" domain. When TLS client certificate
 * authentication is attempted against this domain, it fails because
 * ApplicationDomain cannot verify X.509 evidence.
 */
@Stateless
@RemoteHome(CallerPropHome.class)
public class CallerPropDefaultDomainBean {

    public String hello() {
        return "hello";
    }

    @PostConstruct
    public void postConstruct() {
    }
}
