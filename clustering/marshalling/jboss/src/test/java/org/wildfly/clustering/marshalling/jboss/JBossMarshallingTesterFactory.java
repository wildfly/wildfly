/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.jboss;

import org.jboss.marshalling.MarshallingConfiguration;
import org.junit.Assert;
import org.wildfly.clustering.marshalling.MarshallingTester;
import org.wildfly.clustering.marshalling.MarshallingTesterFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferTestMarshaller;

/**
 * @author Paul Ferraro
 */
public class JBossMarshallingTesterFactory implements MarshallingTesterFactory, MarshallingConfigurationRepository {

    private final MarshallingConfiguration configuration = new MarshallingConfiguration();
    private final ClassLoader loader;

    public JBossMarshallingTesterFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public JBossMarshallingTesterFactory(ClassLoader loader) {
        this.loader = loader;
        this.configuration.setClassTable(new DynamicClassTable(loader));
        this.configuration.setObjectTable(new ExternalizerObjectTable(loader));
    }

    @Override
    public <T> MarshallingTester<T> createTester() {
        return new MarshallingTester<>(new ByteBufferTestMarshaller<>(new JBossByteBufferMarshaller(this, this.loader)));
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return 0;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        Assert.assertEquals(this.getCurrentMarshallingVersion(), version);
        return this.configuration;
    }
}
