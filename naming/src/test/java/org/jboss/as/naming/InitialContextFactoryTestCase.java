/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming;

import static org.junit.Assert.assertTrue;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.spi.NamingManager;

import org.junit.Before;
import org.junit.Test;

/**
 * @author John E. Bailey
 */
public class InitialContextFactoryTestCase {

    @Before
    public void init() {
        NamingContext.setActiveNamingStore(new InMemoryNamingStore());
    }

    @Test
    public void testInitialFactory() throws Exception {
        // Test with sys prop
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactory.class.getName());
        InitialContext initialContext = new InitialContext();
        Context context = (Context) initialContext.lookup("");
        assertTrue(context instanceof NamingContext);

        // Test with builder
        if (!NamingManager.hasInitialContextFactoryBuilder()) {
            NamingManager.setInitialContextFactoryBuilder(new InitialContextFactoryBuilder());
        }
        initialContext = new InitialContext();
        context = (Context) initialContext.lookup("");
        assertTrue(context instanceof NamingContext);
    }

    @Test
    public void testJavaContext() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactory.class.getName());
        System.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.as.naming.interfaces");
        InitialContext initialContext = new InitialContext();
        Context context = (Context) initialContext.lookup("java:");
        assertTrue(context instanceof NamingContext);
    }
}
