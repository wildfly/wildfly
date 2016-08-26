/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.injection.resource.basic;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;

/**
 * This beans checks both methods of getting an ORB.
 *
 * @author carlo
 */
@Stateless
public class CheckORBBean implements CheckORBRemote {
    @Resource
    private ORB orb;

    public void checkForInjectedORB() {
        if (this.orb == null)
            throw new IllegalStateException("ORB was not injected");
        checkORB(orb);
    }

    public void checkForORBInEnvironment() {
        try {
            InitialContext ctx = new InitialContext();
            ORB orb = (ORB) ctx.lookup("java:comp/ORB");
            checkORB(orb);
        } catch (NamingException e) {
            throw new IllegalStateException("Can't lookup java:comp/ORB", e);
        }
    }

    private void checkORB(ORB orb) {
        try {
            POA poa = (POA) orb.resolve_initial_references("RootPOA");
            if (poa == null)
                throw new IllegalStateException("RootPOA is null");
        } catch (InvalidName e) {
            throw new IllegalStateException("Can't resolve RootPOA", e);
        }
    }
}
