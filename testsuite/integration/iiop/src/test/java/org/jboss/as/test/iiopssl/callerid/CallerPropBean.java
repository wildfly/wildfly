/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiopssl.callerid;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

@Stateless
@RemoteHome(CallerPropHome.class)
@SecurityDomain("iiopCert")
@PermitAll
public class CallerPropBean {

    public String hello() {
        return "hello";
    }

    @PostConstruct
    public void postConstruct() {
    }
}
