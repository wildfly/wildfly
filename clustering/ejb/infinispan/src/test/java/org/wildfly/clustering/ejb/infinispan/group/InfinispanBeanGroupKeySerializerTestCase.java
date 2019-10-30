/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan.group;

import java.io.IOException;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Test;
import org.wildfly.clustering.ejb.infinispan.group.InfinispanBeanGroupKeySerializer.InfinispanBeanGroupKeyExternalizer;
import org.wildfly.clustering.ejb.infinispan.group.InfinispanBeanGroupKeySerializer.InfinispanBeanGroupKeyFormat;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormatTester;
import org.wildfly.clustering.marshalling.ExternalizerTester;

/**
 * Unit test for {@link InfinispanBeanGroupKeySerializer}.
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupKeySerializerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        InfinispanBeanGroupKey<SessionID> key = new InfinispanBeanGroupKey<>(new UUIDSessionID(UUID.randomUUID()));

        new ExternalizerTester<>(new InfinispanBeanGroupKeyExternalizer()).test(key);
        new KeyFormatTester<>(new InfinispanBeanGroupKeyFormat()).test(key);
    }
}
