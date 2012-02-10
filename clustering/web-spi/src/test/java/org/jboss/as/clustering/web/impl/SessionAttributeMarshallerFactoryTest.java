/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.as.clustering.web.SessionAttributeMarshallerFactory;
import org.jboss.marshalling.ClassResolver;
import org.jboss.marshalling.MarshallerFactory;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class SessionAttributeMarshallerFactoryTest {
    @Test
    public void test() {
        MarshallerFactory marshallerFactory = mock(MarshallerFactory.class);
        LocalDistributableSessionManager manager = mock(LocalDistributableSessionManager.class);
        SessionAttributeMarshallerFactory factory = new SessionAttributeMarshallerFactoryImpl(marshallerFactory);
        ClassResolver resolver = mock(ClassResolver.class);

        when(manager.getApplicationClassResolver()).thenReturn(resolver);

        SessionAttributeMarshaller marshaller = factory.createMarshaller(manager);

        assertNotNull(marshaller);
        assertTrue(marshaller instanceof SessionAttributeMarshallerImpl);
    }
}
