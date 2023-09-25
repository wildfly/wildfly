/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiopssl.basic;

import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * @author Bartosz Spyrko-Smietanko
 */
@RemoteHome(IIOPSslStatelessHome.class)
@Stateless
public class IIOPSslStatelessBean {

    public String hello() {
        return "hello";
    }

    public void ejbCreate() {

    }

    public void ejbActivate() {

    }

    public void ejbPassivate() {

    }

    public void ejbRemove() {

    }

    public void setSessionContext(SessionContext context) {

    }
}
