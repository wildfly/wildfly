/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.basic;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
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
