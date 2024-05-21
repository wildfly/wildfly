/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ServiceLoader;

import jakarta.annotation.Resource;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.Search;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.single.infinispan.query.data.Book;
import org.jboss.as.test.clustering.single.infinispan.query.data.BookSchema;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for remote query using container-managed objects.
 *
 * @author Radoslav Husar
 * @since 27
 */
@RunWith(Arquillian.class)
@ServerSetup(ServerSetupTask.class)
public class ContainerRemoteQueryTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, ContainerRemoteQueryTestCase.class.getSimpleName() + ".war")
                .addClasses(ContainerRemoteQueryTestCase.class)
                .addPackage(Book.class.getPackage())
                .addAsServiceProvider(SerializationContextInitializer.class.getName(), BookSchema.class.getName() + "Impl")
                .setManifest(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan, org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.protostream, org.infinispan.query, org.infinispan.query.dsl, org.infinispan.query.client").exportAsString()))
                ;
    }

    @Resource(lookup = "java:jboss/infinispan/remote-container/query")
    private RemoteCacheContainer container;

    @Test
    public void testRemoteBookQuery() {
        RemoteCache<String, Book> cache = this.container.getCache();
        cache.clear();

        RemoteCache<String, String> schemaCache = this.container.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        for (SerializationContextInitializer initializer : ServiceLoader.load(SerializationContextInitializer.class, this.getClass().getClassLoader())) {
            if (initializer instanceof GeneratedSchema) {
                GeneratedSchema schema = (GeneratedSchema) initializer;
                schemaCache.put(schema.getProtoFileName(), schema.getProtoFile());
                assertFalse(schemaCache.containsKey(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX));
            }
        }

        QueryFactory queryFactory = Search.getQueryFactory(cache);

        cache.put("A", new Book("A1", "A2", 2021));
        cache.put("B", new Book("B1", "B2", 2022));

        assertTrue(cache.containsKey("A"));

        Query<Book> query = queryFactory.create("FROM Book WHERE author='A2'");

        List<Book> list = query.execute().list();
        assertEquals(1, list.size());
        assertEquals(Book.class, list.get(0).getClass());
        assertEquals("A2", list.get(0).author);
    }
}
