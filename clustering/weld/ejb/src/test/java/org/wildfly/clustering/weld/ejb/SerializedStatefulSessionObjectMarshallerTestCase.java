/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.weld.ejb;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.weld.ejb.SerializedStatefulSessionObject;
import org.jboss.ejb.client.UUIDSessionID;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;

/**
 * @author Paul Ferraro
 */
public class SerializedStatefulSessionObjectMarshallerTestCase {

    @Test
    public void test() throws IOException {
        Tester<SerializedStatefulSessionObject> tester = ProtoStreamTesterFactory.INSTANCE.createTester();
        tester.test(new SerializedStatefulSessionObject(ServiceName.JBOSS.append("foo", "bar"), new UUIDSessionID(UUID.randomUUID()), Map.of(SerializedStatefulSessionObjectMarshallerTestCase.class, ServiceName.of("foo", "bar"))), SerializedStatefulSessionObjectMarshallerTestCase::assertEquals);
    }

    static void assertEquals(SerializedStatefulSessionObject object1, SerializedStatefulSessionObject object2) {
        Assert.assertEquals(object1.getComponentServiceName(), object2.getComponentServiceName());
        Assert.assertEquals(object1.getServiceNames(), object2.getServiceNames());
        Assert.assertEquals(object1.getSessionID(), object2.getSessionID());
    }
}
