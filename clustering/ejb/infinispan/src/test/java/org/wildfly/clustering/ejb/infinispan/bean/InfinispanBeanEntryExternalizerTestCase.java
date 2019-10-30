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

package org.wildfly.clustering.ejb.infinispan.bean;

import java.io.IOException;
import java.util.UUID;

import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.client.UUIDSessionID;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;

/**
 * Unit test for {@link InfinispanBeanEntryExternalizer}.
 * @author Paul Ferraro
 */
public class InfinispanBeanEntryExternalizerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        InfinispanBeanEntry<SessionID> entry = new InfinispanBeanEntry<>("StatefulBean", new UUIDSessionID(UUID.randomUUID()));
        new ExternalizerTester<>(new InfinispanBeanEntryExternalizer(), InfinispanBeanEntryExternalizerTestCase::assertEquals).test(entry);
    }

    static void assertEquals(InfinispanBeanEntry<SessionID> entry1, InfinispanBeanEntry<SessionID> entry2) {
        Assert.assertEquals(entry1.getBeanName(), entry2.getBeanName());
        Assert.assertEquals(entry1.getGroupId(), entry2.getGroupId());
    }
}
