/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
