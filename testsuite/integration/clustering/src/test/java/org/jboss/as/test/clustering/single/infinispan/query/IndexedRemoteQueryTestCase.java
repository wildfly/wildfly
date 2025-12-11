/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_PASSWORD;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_APPLICATION_USER;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_ADDRESS;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteSchemasAdmin;
import org.infinispan.client.hotrod.RemoteSchemasAdmin.SchemaOpResult;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.api.query.Query;
import org.infinispan.commons.configuration.StringConfiguration;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.query.data.Book;
import org.jboss.as.test.clustering.single.infinispan.query.data.BookSchema;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test remote query.
 * Adopted and adapted from Infinispan testsuite (HotRodQueryIT).
 *
 * @author Radoslav Husar
 * @author Adrian Nistor
 * @since 27
 */
@RunWith(Arquillian.class)
public class IndexedRemoteQueryTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, IndexedRemoteQueryTestCase.class.getSimpleName() + ".war")
                .addClasses(IndexedRemoteQueryTestCase.class)
                .addPackage(Book.class.getPackage())
                .addAsServiceProvider(SerializationContextInitializer.class.getName(), BookSchema.class.getName() + "Impl")
                .setManifest(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.protostream").exportAsString()));
    }

    @Test
    public void test() {
        try (RemoteCacheManager container = this.createRemoteCacheManager()) {
            String config = """
{
    "local-cache" : {
        "encoding" : {
            "key" : {
                "media-type" : "application/x-protostream"
            },
            "value" : {
                "media-type" : "application/x-protostream"
            }
        },
        "indexing" : {
            "storage" : "local-heap",
            "indexed-entities" : [ "Book" ]
        }
    }
}""";
            String cacheName = IndexedRemoteQueryTestCase.class.getSimpleName();
            RemoteCache<Integer, Book> cache = container.administration().createCache(cacheName, new StringConfiguration(config));
            try {
                // Add some Books
                Book book1 = new Book("Infinispan in Action", "Learn Infinispan by using it", 2015);
                Book book2 = new Book("Cloud-Native Applications with Java and Quarkus", "Build robust and reliable cloud applications", 2019);

                cache.put(1, book1);
                cache.put(2, book2);

                Query<Book> query = cache.query("FROM Book WHERE title:'java'");
                List<Book> list = query.execute().list();
                assertEquals(1, list.size());
            } finally {
                cache.clear();
                container.administration().reindexCache(cacheName);
                container.administration().removeCache(cacheName);
            }
        }
    }

    private RemoteCacheManager createRemoteCacheManager() {
        List<GeneratedSchema> schemas = ServiceLoader.load(SerializationContextInitializer.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).filter(GeneratedSchema.class::isInstance).map(GeneratedSchema.class::cast).collect(Collectors.toList());
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer().host(INFINISPAN_SERVER_ADDRESS);
        for (GeneratedSchema schema : schemas) {
            builder.addContextInitializer(schema);
        }
        builder.security().authentication().username(INFINISPAN_APPLICATION_USER).password(INFINISPAN_APPLICATION_PASSWORD);
        builder.marshaller(new ProtoStreamMarshaller());
        RemoteCacheManager container = new RemoteCacheManager(builder.build()) {
            @Override
            public void close() {
                RemoteSchemasAdmin admin = this.administration().schemas();
                for (GeneratedSchema schema : schemas) {
                    // Index may still reference the book schema
                    if (!(schema instanceof BookSchema)) {
                        SchemaOpResult result = admin.remove(schema.getName());
                        Assert.assertFalse(result.getError(), result.hasError());
                    }
                }
                super.close();
            }
        };
        RemoteSchemasAdmin admin = container.administration().schemas();
        for (GeneratedSchema schema : schemas) {
            SchemaOpResult result = admin.create(schema);
            Assert.assertFalse(result.getError(), result.hasError());
        }
        return container;
    }
}
