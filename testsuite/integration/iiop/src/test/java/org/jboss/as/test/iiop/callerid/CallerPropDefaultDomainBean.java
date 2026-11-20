/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.callerid;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;

@Stateless
@RemoteHome(CallerPropHome.class)
@PermitAll
public class CallerPropDefaultDomainBean {

    public String hello() {
        return "hello";
    }

    @PostConstruct
    public void postConstruct() {
    }
}
