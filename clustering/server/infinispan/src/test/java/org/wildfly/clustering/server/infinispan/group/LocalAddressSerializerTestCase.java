/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.infinispan.group;

import java.io.IOException;

import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.LocalModeAddress;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.marshalling.spi.FormatterTester;
import org.wildfly.clustering.server.infinispan.group.LocalAddressSerializer.LocalAddressExternalizer;
import org.wildfly.clustering.server.infinispan.group.LocalAddressSerializer.LocalAddressFormatter;

/**
 * @author Paul Ferraro
 */
public class LocalAddressSerializerTestCase {

    @Test
    public void test() throws IOException {
        test(new ExternalizerTester<>(new LocalAddressExternalizer()));
        test(new FormatterTester<>(new LocalAddressFormatter()));
        test(JBossMarshallingTesterFactory.INSTANCE.createTester());
        test(ProtoStreamTesterFactory.INSTANCE.createTester());
    }

    static void test(Tester<Address> tester) throws IOException {
        tester.test(LocalModeAddress.INSTANCE);
    }
}
