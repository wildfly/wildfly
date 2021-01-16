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

package org.wildfly.clustering.server.singleton;

import java.io.IOException;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartException;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormatTester;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.Tester;
import org.wildfly.clustering.marshalling.jboss.JBossMarshallingTesterFactory;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamTesterFactory;
import org.wildfly.clustering.server.singleton.ServiceNameResolver.ServiceNameExternalizer;
import org.wildfly.clustering.server.singleton.ServiceNameResolver.ServiceNameKeyFormat;

/**
 * Unit test for {@link ServiceNameResolver}.
 * @author Paul Ferraro
 */
public class ServiceNameResolverTestCase {
    private final ServiceName name = ServiceName.JBOSS.append("foo", "bar");

    @Test
    public void test() throws IOException {
        new ExternalizerTester<>(new ServiceNameExternalizer()).test(this.name);
        new KeyFormatTester<>(new ServiceNameKeyFormat()).test(this.name);

        new JBossMarshallingTesterFactory(this.getClass().getClassLoader()).createTester().test(this.name);
        new ProtoStreamTesterFactory(this.getClass().getClassLoader()).createTester().test(this.name);

        Tester<Throwable> tester = new ProtoStreamTesterFactory(this.getClass().getClassLoader()).createTester();
        tester.test(new StartException(), ServiceNameResolverTestCase::assertEquals);
        tester.test(new StartException("message"), ServiceNameResolverTestCase::assertEquals);
        tester.test(new StartException(new Exception()), ServiceNameResolverTestCase::assertEquals);
        tester.test(new StartException("message", new Exception()), ServiceNameResolverTestCase::assertEquals);

        tester.test(new ServiceNotFoundException(), ServiceNameResolverTestCase::assertEquals);
        tester.test(new ServiceNotFoundException("message"), ServiceNameResolverTestCase::assertEquals);
        tester.test(new ServiceNotFoundException(new Exception()), ServiceNameResolverTestCase::assertEquals);
        tester.test(new ServiceNotFoundException("message", new Exception()), ServiceNameResolverTestCase::assertEquals);
    }

    private static void assertEquals(Throwable exception1, Throwable exception2) {
        if ((exception1 != null) && (exception2 != null)) {
            Assert.assertSame(exception1.getClass(), exception2.getClass());
            Assert.assertEquals(exception1.getMessage(), exception2.getMessage());
            assertEquals(exception1.getCause(), exception2.getCause());
        } else {
            Assert.assertSame(exception1, exception2);
        }
    }
}
