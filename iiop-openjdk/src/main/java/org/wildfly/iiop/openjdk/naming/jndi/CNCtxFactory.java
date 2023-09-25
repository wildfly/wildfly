/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk.naming.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * Implements the JNDI SPI InitialContextFactory interface used to
 * create  the InitialContext objects.
 *
 * @author Raj Krishnamurthy
 */

public class CNCtxFactory implements InitialContextFactory {

    public static final CNCtxFactory INSTANCE = new CNCtxFactory();

    /**
     * Creates the InitialContext object. Properties parameter should
     * should contain the ORB object for the value jndi.corba.orb.
     *
     * @param env Properties object
     */

    public Context getInitialContext(Hashtable<?, ?> env) throws NamingException {
        return new CNCtx(env);
    }
}
