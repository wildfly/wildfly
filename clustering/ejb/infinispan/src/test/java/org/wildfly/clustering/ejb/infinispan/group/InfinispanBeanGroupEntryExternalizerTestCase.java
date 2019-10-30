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
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshalledValue;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContext;

/**
 * Unit test for {@link InfinispanBeanGroupEntryExternalizer}.
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupEntryExternalizerTestCase {

    private static final MarshallerFactory factory = Marshalling.getMarshallerFactory("river", InfinispanBeanGroupEntryExternalizerTestCase.class.getClassLoader());
    private static final MarshallingConfigurationRepository repository = new SimpleMarshallingConfigurationRepository(new MarshallingConfiguration());
    private static final MarshallingContext context = new SimpleMarshallingContext(factory, repository, InfinispanBeanGroupEntryExternalizerTestCase.class.getClassLoader());

    @Test
    public void test() throws ClassNotFoundException, IOException {
        SessionID id = new UUIDSessionID(UUID.randomUUID());
        Map<SessionID, String> beans = Collections.singletonMap(id, "bean");
        InfinispanBeanGroupEntry<SessionID, String> entry = new InfinispanBeanGroupEntry<>(new SimpleMarshalledValue<>(beans, context));
        new ExternalizerTester<>(new InfinispanBeanGroupEntryExternalizer<>(), InfinispanBeanGroupEntryExternalizerTestCase::assertEquals).test(entry);
    }

    static void assertEquals(InfinispanBeanGroupEntry<SessionID, String> entry1, InfinispanBeanGroupEntry<SessionID, String> entry2) {
        try {
            Assert.assertEquals(entry1.getBeans().get(context), entry2.getBeans().get(context));
        } catch (ClassNotFoundException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
