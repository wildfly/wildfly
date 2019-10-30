/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.servlet;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Stateless;

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
