/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;
import static org.junit.jupiter.api.Assertions.*;

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
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
import org.jboss.as.test.clustering.single.infinispan.query.data.PersonSchema;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test the Infinispan AS remote client module integration.
 * Adopted and adapted from Infinispan testsuite (HotRodClientIT).
 *
 * @author Radoslav Husar
 * @author Pedro Ruivo
 * @since 27
 */
@ExtendWith(ArquillianExtension.class)
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

    @BeforeEach
    void initialize() {

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
            assertFalse(result.hasError(), result.getError());
        }
    }

    @AfterEach
    void destroy() {
        try (RemoteCacheManager manager = this.remoteCacheManager) {
            RemoteSchemasAdmin admin = manager.administration().schemas();
            for (GeneratedSchema schema : this.schemas) {
                SchemaOpResult result = admin.remove(schema.getName());
                assertFalse(result.hasError(), result.getError());
            }
        }
    }

    @Test
    void putGetCustomObject() {
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
