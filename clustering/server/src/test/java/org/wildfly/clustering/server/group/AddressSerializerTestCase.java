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

package org.wildfly.clustering.server.group;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.Address;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.IpAddressUUID;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTesterFactory;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.server.group.AddressSerializer.IpAddressExternalizer;
import org.wildfly.clustering.server.group.AddressSerializer.IpAddressUUIDExternalizer;
import org.wildfly.clustering.server.group.AddressSerializer.UUIDExternalizer;

/**
 * @author Paul Ferraro
 */
public class AddressSerializerTestCase {

    @Test
    public void test() throws IOException {
        test(new ExternalizerTesterFactory(new UUIDExternalizer(), new IpAddressExternalizer(), new IpAddressUUIDExternalizer()).createTester());
        test(new JBossMarshallingTesterFactory(this.getClass().getClassLoader()).createTester());
        test(new ProtoStreamTesterFactory(this.getClass().getClassLoader()).createTester());
    }

    private static void test(Tester<Address> tester) throws IOException {
        UUID uuid = UUID.randomUUID();
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), Short.MAX_VALUE);
        IpAddress ipAddress = new IpAddress(address);
        IpAddressUUID ipAddressUUID = new IpAddressUUID(address);

        tester.test(uuid);
        tester.test(ipAddress);
        tester.test(ipAddressUUID);
    }
}
