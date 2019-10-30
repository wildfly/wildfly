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

import java.util.Hashtable;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class ObjectFactoryTestCase {
    private WritableNamingStore namingStore;
    private NamingContext namingContext;

    @BeforeClass
    public static void initNamingManager() throws Exception {
        NamingContext.initializeNamingManager();
    }

    @Before
    public void setup() throws Exception {
        namingStore = new InMemoryNamingStore();
        NamingContext.setActiveNamingStore(namingStore);
        namingContext = new NamingContext(namingStore, null);
    }

    @After
    public void cleanup() throws Exception {
        NamingContext.setActiveNamingStore(new InMemoryNamingStore());
    }

    @Test
    public void testBindAndRetrieveObjectFactoryFromNamingContext() throws Exception {
        final Reference reference = new Reference("java.util.String", TestObjectFactory.class.getName(), null);
        namingStore.bind(new CompositeName("test"), reference);

        final Object result = namingContext.lookup("test");
        assertTrue(result instanceof String);
        assertEquals("Test ParsedResult", result);
    }

    @Test
    public void testBindAndRetrieveObjectFactoryFromInitialContext() throws Exception {
        final Reference reference = new Reference("java.util.String", TestObjectFactory.class.getName(), null);
        namingStore.bind(new CompositeName("test"), reference);

        final InitialContext initialContext = new InitialContext();
        final Object result = initialContext.lookup("test");
        assertTrue(result instanceof String);
        assertEquals("Test ParsedResult", result);
    }


    public static class TestObjectFactory implements ObjectFactory {
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
            return "Test ParsedResult";
        }
    }
}
