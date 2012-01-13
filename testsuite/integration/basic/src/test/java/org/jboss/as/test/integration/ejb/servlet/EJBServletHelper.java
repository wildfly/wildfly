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
