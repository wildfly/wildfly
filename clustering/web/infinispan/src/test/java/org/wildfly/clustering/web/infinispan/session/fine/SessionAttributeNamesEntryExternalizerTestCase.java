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

package org.wildfly.clustering.web.infinispan.session.fine;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;

/**
 * Unit test for {@link SessionAttributeNamesEntryExternalizer}.
 * @author Paul Ferraro
 */
public class SessionAttributeNamesEntryExternalizerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        ConcurrentMap<String, Integer> attributes = new ConcurrentHashMap<>();
        attributes.put("a", 1);
        attributes.put("b", 2);
        attributes.put("c", 3);
        SessionAttributeNamesEntry entry = new SessionAttributeNamesEntry(new AtomicInteger(10), attributes);
        new ExternalizerTester<>(new SessionAttributeNamesEntryExternalizer(), SessionAttributeNamesEntryExternalizerTestCase::assertEquals).test(entry);
    }

    static void assertEquals(SessionAttributeNamesEntry entry1, SessionAttributeNamesEntry entry2) {
        Assert.assertEquals(entry1.getNames(), entry2.getNames());
        Assert.assertEquals(entry1.getSequence().get(), entry2.getSequence().get());
    }
}
