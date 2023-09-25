/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_PASSWORD;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_USER;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_ADDRESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
import org.jboss.as.test.clustering.single.infinispan.query.data.PersonSchema;
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

    private RemoteCache<String, Person> remoteCache;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, HotRodClientTestCase.class.getSimpleName() + ".war")
                .addClass(HotRodClientTestCase.class)
                .addPackage(Person.class.getPackage())
                .addAsServiceProvider(SerializationContextInitializer.class.getName(), PersonSchema.class.getName() + "Impl")
                .setManifest(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan, org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.query, org.infinispan.protostream").exportAsString()))
                ;
    }

    @Before
    public void initialize() {
        List<GeneratedSchema> schemas = ServiceLoader.load(SerializationContextInitializer.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).filter(GeneratedSchema.class::isInstance).map(GeneratedSchema.class::cast).collect(Collectors.toList());

        ConfigurationBuilder config = new ConfigurationBuilder();
        config.addServer().host(INFINISPAN_SERVER_ADDRESS);
        config.marshaller(new ProtoStreamMarshaller());
        for (GeneratedSchema schema : schemas) {
            config.addContextInitializer(schema);
        }
        config.security().authentication().username(INFINISPAN_APPLICATION_USER).password(INFINISPAN_APPLICATION_PASSWORD);

        RemoteCacheManager remoteCacheManager = new RemoteCacheManager(config.build(), true);
        this.remoteCache = remoteCacheManager.getCache();
        this.remoteCache.clear();

        RemoteCache<String, String> schemaCache = this.remoteCache.getRemoteCacheContainer().getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        for (GeneratedSchema schema : schemas) {
            schemaCache.put(schema.getProtoFileName(), schema.getProtoFile());
            assertFalse(schemaCache.containsKey(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX));
        }
    }

    @Test
    public void testPutGetCustomObject() {
        String key = "k1";
        Person expected = new Person("Martin");
        this.remoteCache.put(key, expected);
        Person result = this.remoteCache.get(key);
        assertNotNull(result);
        assertEquals(expected.getName(), result.getName());
    }
}
