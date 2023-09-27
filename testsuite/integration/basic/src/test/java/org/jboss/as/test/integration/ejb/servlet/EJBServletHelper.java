/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.servlet;

import javax.naming.InitialContext;

/**
 * Implements processRequest method.
 */
public class EJBServletHelper {

    protected void processRequest(String lookupString, InitialContext ctx) throws Exception {

        Session30BusinessRemote remote = (Session30BusinessRemote) ctx.lookup(lookupString + Session30BusinessRemote.class.getName());

        remote.hello();
        remote.goodbye();

        TestObject o = new TestObject();
        remote.access(o);
        o = remote.createTestObject();

        Session30BusinessLocal local = (Session30BusinessLocal) ctx.lookup(lookupString + Session30BusinessLocal.class.getName());
        o = new TestObject();
        local.access(o);
        o = local.createTestObject();
        local.getWarTestObject();

        Session30Home home = (Session30Home) ctx.lookup(lookupString + Session30Home.class.getName());
        Session30Remote remote21 = home.create();
        remote21.access(o);

        Session30LocalHome localHome = (Session30LocalHome) ctx.lookup(lookupString + Session30LocalHome.class.getName());
        Session30Local local21 = localHome.create();
        local21.access(o);

        home = (Session30Home) ctx.lookup("java:comp/env/ejb/remote/Session30");
        remote21 = home.create();
        remote21.access(o);

        localHome = (Session30LocalHome) ctx.lookup("java:comp/env/ejb/local/Session30");
        local21 = localHome.create();
        local21.access(o);
    }
}
