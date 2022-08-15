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

package org.jboss.as.test.clustering.single.infinispan.query;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_PASSWORD;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_USER;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the Infinispan AS remote client module integration.
 * Adopted and adapted from Infinispan testsuite (HotRodClientIT).
 *
 * @author Radoslav Husar
 * @author Pedro Ruivo
 * @since 27
 */
@RunWith(Arquillian.class)
public class HotRodClientTestCase {

    private RemoteCache<String, Object> remoteCache;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, HotRodClientTestCase.class.getSimpleName() + ".war")
                .addClass(HotRodClientTestCase.class)
                .addClass(Person.class)
                .add(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan, org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.query, org.infinispan.protostream").exportAsString()), "META-INF/MANIFEST.MF");
    }

    private static RemoteCache<String, Object> createRemoteCache() {
        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(createConfiguration(), true);
        RemoteCache<String, Object> remoteCache = remoteCacheManager.getCache();
        remoteCache.clear();
        return remoteCache;
    }

    private static Configuration createConfiguration() {
        ConfigurationBuilder config = new ConfigurationBuilder();
        config.addServer().host("127.0.0.1");
        config.marshaller(new ProtoStreamMarshaller());
        config.security().authentication().username(INFINISPAN_APPLICATION_USER).password(INFINISPAN_APPLICATION_PASSWORD);
        return config.build();
    }

    @Before
    public void initialize() {
        remoteCache = createRemoteCache();
    }

    @Test
    public void testPutGetCustomObject() throws IOException {
        SerializationContext serializationContext = MarshallerUtil.getSerializationContext(remoteCache.getRemoteCacheManager());
        ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
        String protoFile = protoSchemaBuilder.fileName("test.proto")
                .addClass(Person.class)
                .build(serializationContext);
        remoteCache.put("test.proto", protoFile);

        final Person p = new Person("Martin");
        remoteCache.put("k1", p);
        assertEquals(p.getName(), ((Person) remoteCache.get("k1")).getName());
    }
}
