/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.servlet;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.LocalHome;
import jakarta.ejb.Remote;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateless(name = "Session30")
@Remote(Session30BusinessRemote.class)
@Local(Session30BusinessLocal.class)
@RemoteHome(Session30Home.class)
@LocalHome(Session30LocalHome.class)
@SecurityDomain("other")
public class Session30Bean implements Session30 {

    @EJB
    private StatefulRemote stateful;

    private TestObject testObject;

    @RolesAllowed({ "Role1" })
    public void hello() {
    }

    @RolesAllowed({ "Role1" })
    public void goodbye() {
    }

    public String access(TestObject o) {
        return stateful.access(o);
    }

    public TestObject createTestObject() {
        testObject = new TestObject();
        return testObject;
    }

    public boolean checkEqPointer(TestObject to) {
        return to == testObject;
    }

    public WarTestObject getWarTestObject() {
        return new WarTestObject();
    }

}
