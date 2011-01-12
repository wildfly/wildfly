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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.Reference;

import org.jboss.as.naming.context.ObjectFactoryBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class ServiceReferenceObjectFactoryTestCase {
    private NamingContext namingContext;

    private static final ServiceName testServiceName = ServiceName.of("test", "service");

    @BeforeClass
    public static void init() throws Exception {
        NamingContext.initializeNamingManager();
        ServiceContainer container = ServiceContainer.Factory.create("naming-test");
        container.addService(testServiceName, new Service<String>() {

            @Override
            public void start(StartContext context) throws StartException {
            }

            @Override
            public void stop(StopContext context) {
            }

            @Override
            public String getValue() throws IllegalStateException, IllegalArgumentException {
                return "hello world";
            }
        }).install();
        ObjectFactoryBuilder.INSTANCE.setServiceRegistry(container);
    }

    @Before
    public void setup() throws Exception {
        namingContext = new NamingContext(null);
    }

    @After
    public void cleanup() throws Exception {
        NamingContext.setActiveNamingStore(new InMemoryNamingStore());
    }

    @Test
    public void testBindAndRetrieveObjectFactoryFromNamingContext() throws Exception {
        final Reference reference = ServiceValueObjectFactory.createReference(testServiceName, ServiceValueObjectFactory.class);
        namingContext.bind("test", reference);

        final Object result = namingContext.lookup("test");
        assertTrue(result instanceof String);
        assertEquals("hello world", result);
    }

    @Test
    public void testBindAndRetrieveObjectFactoryFromInitialContext() throws Exception {
        final Reference reference = ServiceValueObjectFactory.createReference(testServiceName, ServiceValueObjectFactory.class);
        final InitialContext initialContext = new InitialContext();
        initialContext.bind("test", reference);

        final Object result = namingContext.lookup("test");
        assertTrue(result instanceof String);
        assertEquals("hello world", result);
    }


}
