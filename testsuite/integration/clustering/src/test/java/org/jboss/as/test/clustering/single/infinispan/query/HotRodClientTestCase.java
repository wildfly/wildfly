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
import org.infinispan.client.hotrod.RemoteSchemasAdmin;
import org.infinispan.client.hotrod.RemoteSchemasAdmin.SchemaOpResult;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContextInitializer;
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
import org.junit.After;
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
    private RemoteCacheManager remoteCacheManager;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, HotRodClientTestCase.class.getSimpleName() + ".war")
                .addClass(HotRodClientTestCase.class)
                .addPackage(Person.class.getPackage())
                .addAsServiceProvider(SerializationContextInitializer.class.getName(), PersonSchema.class.getName() + "Impl")
                .setManifest(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.protostream").exportAsString()));
    }

    private final List<GeneratedSchema> schemas = ServiceLoader.load(SerializationContextInitializer.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).filter(GeneratedSchema.class::isInstance).map(GeneratedSchema.class::cast).collect(Collectors.toList());

    @Before
    public void initialize() {

        ConfigurationBuilder config = new ConfigurationBuilder();
        config.addServer().host(INFINISPAN_SERVER_ADDRESS);
        for (GeneratedSchema schema : this.schemas) {
            config.addContextInitializer(schema);
        }
        config.security().authentication().username(INFINISPAN_APPLICATION_USER).password(INFINISPAN_APPLICATION_PASSWORD);

        this.remoteCacheManager = new RemoteCacheManager(config.build(), true);

        RemoteSchemasAdmin admin = this.remoteCacheManager.administration().schemas();
        for (GeneratedSchema schema : this.schemas) {
            SchemaOpResult result = admin.create(schema);
            assertFalse(result.getError(), result.hasError());
        }
    }

    @After
    public void destroy() {
        try (RemoteCacheManager manager = this.remoteCacheManager) {
            RemoteSchemasAdmin admin = manager.administration().schemas();
            for (GeneratedSchema schema : this.schemas) {
                SchemaOpResult result = admin.remove(schema.getName());
                assertFalse(result.getError(), result.hasError());
            }
        }
    }

    @Test
    public void testPutGetCustomObject() {
        String key = "k1";
        Person expected = new Person("Martin");
        RemoteCache<String, Person> cache = this.remoteCacheManager.getCache("query");
        try {
            cache.put(key, expected);
            Person result = cache.get(key);
            assertNotNull(result);
            assertEquals(expected.getName(), result.getName());
        } finally {
            cache.clear();
        }
    }
}
