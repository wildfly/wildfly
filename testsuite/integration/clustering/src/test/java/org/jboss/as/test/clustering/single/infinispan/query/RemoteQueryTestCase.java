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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.query.data.Book;
import org.jboss.as.test.clustering.single.infinispan.query.proto.BookQuerySchema;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
import org.jboss.as.test.clustering.single.infinispan.query.proto.BookQuerySchemaImpl;
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

    private static final String HOST = "127.0.0.1";

    @Deployment
    public static Archive<?> deployment() throws IOException {
        return ShrinkWrap
                .create(WebArchive.class, RemoteQueryTestCase.class.getSimpleName() + ".war")
                .addClasses(RemoteQueryTestCase.class, RemoteQueryTestCase.class, Person.class, Book.class)
                .addPackage(BookQuerySchema.class.getPackage().getName())
                .addAsResource("proto/book.proto")
                .add(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan, org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.protostream, org.infinispan.query, org.infinispan.query.dsl").exportAsString()), "META-INF/MANIFEST.MF");
    }

    @Test
    public void testIndexed() {
        BookQuerySchema schema = new BookQuerySchemaImpl();
        ConfigurationBuilder config = localServerConfiguration();
        config.addContextInitializer(schema);

        try (RemoteCacheManager remoteCacheManager = new RemoteCacheManager(config.build())) {

            // Register schema
            registerSchema(remoteCacheManager, schema.getProtoFileName(), schema.getProtoFile());

            String xmlConfig = "" +
                    "<local-cache name=\"books\">\n" +
                    "  <indexing path=\"${java.io.tmpdir}/index\">\n" +
                    "    <indexed-entities>\n" +
                    "      <indexed-entity>Book</indexed-entity>\n" + //"      <indexed-entity>book_sample.Book</indexed-entity>\n" +
                    "    </indexed-entities>\n" +
                    "  </indexing>\n" +
                    "</local-cache>";
            RemoteCache<Object, Object> remoteCache = remoteCacheManager.administration().getOrCreateCache("books", new XMLStringConfiguration(xmlConfig));

            // Add some Books
            Book book1 = new Book("Infinispan in Action", "Learn Infinispan by using it", 2015);
            Book book2 = new Book("Cloud-Native Applications with Java and Quarkus", "Build robust and reliable cloud applications", 2019);

            remoteCache.put(1, book1);
            remoteCache.put(2, book2);

            QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
            //Query<Book> query = queryFactory.create("FROM book_sample.Book WHERE title:'java'");
            Query<Book> query = queryFactory.create("FROM Book WHERE title:'java'");
            List<Book> list = query.execute().list();
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testRemoteQuery() throws Exception {
        ConfigurationBuilder config = localServerConfiguration();
        config.marshaller(new ProtoStreamMarshaller());
        try (RemoteCacheManager rcm = new RemoteCacheManager(config.build())) {
            // register schema
            SerializationContext serializationContext = MarshallerUtil.getSerializationContext(rcm);
            String protoKey = "test.proto";
            ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
            String protoFile = protoSchemaBuilder.fileName(protoKey)
                    .addClass(Person.class)
                    .build(serializationContext);
            registerSchema(rcm, protoKey, protoFile);

            RemoteCache<String, Person> cache = rcm.getCache();
            cache.clear();
            cache.put("Adrian", new Person("Adrian"));

            assertTrue(cache.containsKey("Adrian"));

            QueryFactory qf = Search.getQueryFactory(cache);
            Query<Person> query = qf.from(Person.class)
                    .having("name").eq("Adrian")
                    .build();
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
        ConfigurationBuilder config = localServerConfiguration();
        config.marshaller(new ProtoStreamMarshaller());
        try (RemoteCacheManager rcm = new RemoteCacheManager(config.build())) {
            // register schema
            SerializationContext serializationContext = MarshallerUtil.getSerializationContext(rcm);
            String protoKey = "test.proto";
            ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
            String protoFile = protoSchemaBuilder.fileName(protoKey)
                    .addClass(Person.class)
                    .build(serializationContext);
            registerSchema(rcm, protoKey, protoFile);

            RemoteCache<String, Person> cache = rcm.getCache();
            cache.clear();

            QueryFactory qf = Search.getQueryFactory(cache);
            Query<Person> query = qf.from(Person.class)
                    .having("name").eq("John")
                    .orderBy("id")
                    .build();
            Assert.assertEquals(0, query.execute().list().size());
        }
    }

    private ConfigurationBuilder localServerConfiguration() {
        ConfigurationBuilder config = new ConfigurationBuilder();
        config.addServer().host(HOST);
        config.security().authentication().username(INFINISPAN_APPLICATION_USER).password(INFINISPAN_APPLICATION_PASSWORD);
        return config;
    }

    private void registerSchema(RemoteCacheManager rcm, String key, String protoFile) {
        RemoteCache<String, String> metadataCache = rcm.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        metadataCache.put(key, protoFile);
        assertFalse(metadataCache.containsKey(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX));
    }
}
