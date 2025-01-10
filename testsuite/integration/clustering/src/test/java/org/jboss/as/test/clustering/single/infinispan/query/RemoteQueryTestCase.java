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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.configuration.StringConfiguration;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.query.data.Book;
import org.jboss.as.test.clustering.single.infinispan.query.data.BookSchema;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
import org.jboss.as.test.clustering.single.infinispan.query.data.PersonSchema;
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
public class RemoteQueryTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, RemoteQueryTestCase.class.getSimpleName() + ".war")
                .addClasses(RemoteQueryTestCase.class)
                .addPackage(Book.class.getPackage())
                .addAsServiceProvider(SerializationContextInitializer.class.getName(), BookSchema.class.getName() + "Impl", PersonSchema.class.getName() + "Impl")
                .setManifest(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan, org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.protostream, org.infinispan.query, org.infinispan.query.dsl").exportAsString()));
    }

    @Test
    public void testIndexed() {
        try (RemoteCacheManager container = this.createRemoteCacheManager()) {
            String xmlConfig =
                    "<local-cache name=\"books\">\n" +
                    "  <indexing storage=\"local-heap\">\n" +
                    "    <indexed-entities>\n" +
                    "      <indexed-entity>Book</indexed-entity>\n" +
                    "    </indexed-entities>\n" +
                    "  </indexing>\n" +
                    "</local-cache>";
            RemoteCache<Integer, Book> remoteCache = container.administration().getOrCreateCache("books", new StringConfiguration(xmlConfig));
            remoteCache.clear();

            // Add some Books
            Book book1 = new Book("Infinispan in Action", "Learn Infinispan by using it", 2015);
            Book book2 = new Book("Cloud-Native Applications with Java and Quarkus", "Build robust and reliable cloud applications", 2019);

            remoteCache.put(1, book1);
            remoteCache.put(2, book2);

            QueryFactory factory = Search.getQueryFactory(remoteCache);
            Query<Book> query = factory.create("FROM Book WHERE title:'java'");
            List<Book> list = query.execute().list();
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testRemoteQuery() throws Exception {
        try (RemoteCacheManager container = this.createRemoteCacheManager()) {
            RemoteCache<String, Person> cache = container.getCache();
            cache.clear();
            cache.put("Adrian", new Person("Adrian"));

            assertTrue(cache.containsKey("Adrian"));

            QueryFactory factory = Search.getQueryFactory(cache);
            Query<Person> query = factory.create("FROM Person WHERE name='Adrian'");
            List<Person> list = query.execute().list();
            assertNotNull(list);
            assertEquals(1, list.size());
            assertEquals(Person.class, list.get(0).getClass());
            assertEquals("Adrian", list.get(0).name);
        }
    }

    /**
     * Sorting on a field that does not contain DocValues so Hibernate Search is forced to uninvert it - ISPN-5729
     */
    @Test
    public void testUninverting() throws Exception {
        try (RemoteCacheManager container = this.createRemoteCacheManager()) {
            RemoteCache<String, Person> cache = container.getCache();
            cache.clear();

            QueryFactory factory = Search.getQueryFactory(cache);
            Query<Person> query = factory.create("FROM Person WHERE name='John' ORDER BY id");
            Assert.assertEquals(0, query.execute().list().size());
        }
    }

    private RemoteCacheManager createRemoteCacheManager() {
        List<GeneratedSchema> schemas = ServiceLoader.load(SerializationContextInitializer.class, this.getClass().getClassLoader()).stream().map(ServiceLoader.Provider::get).filter(GeneratedSchema.class::isInstance).map(GeneratedSchema.class::cast).collect(Collectors.toList());

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer().host(INFINISPAN_SERVER_ADDRESS);
        builder.marshaller(new ProtoStreamMarshaller());
        for (GeneratedSchema schema : schemas) {
            builder.addContextInitializer(schema);
        }
        builder.security().authentication().username(INFINISPAN_APPLICATION_USER).password(INFINISPAN_APPLICATION_PASSWORD);
        RemoteCacheManager container = new RemoteCacheManager(builder.build());
        RemoteCache<String, String> schemaCache = container.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        for (GeneratedSchema schema : schemas) {
            schemaCache.put(schema.getProtoFileName(), schema.getProtoFile());
            assertFalse(schemaCache.containsKey(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX));
        }
        return container;
    }
}
