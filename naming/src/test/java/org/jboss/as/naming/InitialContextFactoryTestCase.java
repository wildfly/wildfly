/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
