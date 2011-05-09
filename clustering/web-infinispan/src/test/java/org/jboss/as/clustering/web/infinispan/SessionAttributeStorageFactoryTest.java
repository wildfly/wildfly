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
package org.jboss.as.clustering.web.infinispan;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class SessionAttributeStorageFactoryTest {
    @Test
    public void testSession() {
        this.test(ReplicationGranularity.SESSION, CoarseSessionAttributeStorage.class);
    }

    @Test
    public void testAttribute() {
        this.test(ReplicationGranularity.ATTRIBUTE, FineSessionAttributeStorage.class);
    }

    @Test
    public void testField() {
        this.test(ReplicationGranularity.FIELD, CoarseSessionAttributeStorage.class);
    }

    private void test(ReplicationGranularity granularity, Class<? extends SessionAttributeStorage<?>> expectedClass) {
        SessionAttributeMarshaller marshaller = mock(SessionAttributeMarshaller.class);

        SessionAttributeStorageFactory factory = new SessionAttributeStorageFactoryImpl();

        try {
            SessionAttributeStorage<?> storage = factory.createStorage(granularity, marshaller);

            assertNotNull(expectedClass);
            assertTrue(storage.getClass().getName(), expectedClass.isInstance(storage));
        } catch (IllegalArgumentException e) {
            assertNull(expectedClass);
        }
    }
}
