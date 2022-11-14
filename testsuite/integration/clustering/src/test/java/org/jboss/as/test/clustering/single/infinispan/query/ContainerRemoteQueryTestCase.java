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
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_ADDRESS;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ServiceLoader;

import jakarta.annotation.Resource;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.single.infinispan.query.data.Book;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
import org.jboss.as.test.clustering.single.infinispan.query.proto.BookQuerySchema;
import org.jboss.as.test.shared.ManagementServerSetupTask;
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
@ServerSetup({ContainerRemoteQueryTestCase.ServerSetupTask.class})
public class ContainerRemoteQueryTestCase {

    static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super("default", createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:add(port=%d, host=%s)", INFINISPAN_SERVER_PORT, INFINISPAN_SERVER_ADDRESS)
                            .add("/subsystem=infinispan/remote-cache-container=query:add(default-remote-cluster=infinispan-server-cluster, tcp-keep-alive=true, marshaller=PROTOSTREAM, modules=[org.infinispan.query.client], properties={infinispan.client.hotrod.auth_username=%s, infinispan.client.hotrod.auth_password=%s}, statistics-enabled=true)", INFINISPAN_APPLICATION_USER, INFINISPAN_APPLICATION_PASSWORD)
                            .add("/subsystem=infinispan/remote-cache-container=query/remote-cluster=infinispan-server-cluster:add(socket-bindings=[infinispan-server])")
                            .endBatch()
                            .build()
                    )
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=infinispan/remote-cache-container=query:remove")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:remove")
                            .endBatch()
                            .build())
                    .build()
            );
        }
    }

    @Deployment
    public static Archive<?> deployment() throws IOException, URISyntaxException {
        return ShrinkWrap
                .create(WebArchive.class, ContainerRemoteQueryTestCase.class.getSimpleName() + ".war")
                .addClasses(ContainerRemoteQueryTestCase.class, ContainerRemoteQueryTestCase.class)
                .addClasses(Person.class, Book.class)
                .addPackage(BookQuerySchema.class.getPackage().getName())
                .addAsResource("proto/book.proto")
                .addAsServiceProvider(SerializationContextInitializer.class.getName(), BookQuerySchema.class.getName() + "Impl")
                .add(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan meta-inf, org.infinispan.commons meta-inf, org.infinispan.client.hotrod meta-inf, org.infinispan.protostream meta-inf, org.infinispan.query meta-inf, org.infinispan.query.dsl meta-inf,org.infinispan.query.client meta-inf, org.infinispan.query.core meta-inf").exportAsString()), "META-INF/MANIFEST.MF")
                ;
    }

    @Resource(lookup = "java:jboss/infinispan/remote-container/query")
    private RemoteCacheContainer remoteCacheContainer;

    @Test
    public void testRemoteBookQuery() {
        RemoteCache<String, Book> cache = remoteCacheContainer.getCache();

        SerializationContext serializationContext = MarshallerUtil.getSerializationContext(cache.getRemoteCacheManager());

        for (SerializationContextInitializer serializationContextInitializer : ServiceLoader.load(SerializationContextInitializer.class)) {
            serializationContextInitializer.registerSchema(serializationContext);
            serializationContextInitializer.registerMarshallers(serializationContext);
        }

        QueryFactory queryFactory = Search.getQueryFactory(cache);

        cache.clear();

        String protoKey = "book.proto";
        String protoFile = "// File name: book.proto\n" +
                "// Generated from : org.jboss.as.test.clustering.single.infinispan.query.proto.BookQuerySchema\n" +
                "\n" +
                "syntax = \"proto2\";\n" +
                "\n" +
                "\n" +
                "\n" +
                "/**\n" +
                " * @Indexed\n" +
                " */\n" +
                "message Book {\n" +
                "   \n" +
                "   /**\n" +
                "    * @Field(index=Index.YES, analyze = Analyze.YES, store = Store.NO)\n" +
                "    */\n" +
                "   optional string title = 1;\n" +
                "   \n" +
                "   /**\n" +
                "    * @Field(index=Index.YES, analyze = Analyze.NO, store = Store.NO)\n" +
                "    */\n" +
                "   optional string author = 2;\n" +
                "   \n" +
                "   /**\n" +
                "    * @Field(index=Index.YES, store = Store.NO)\n" +
                "    */\n" +
                "   optional int32 publicationYear = 3 [default = 0];\n" +
                "}\n";

        // Yuck!
        RemoteCache<String, String> metadataCache = remoteCacheContainer.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        metadataCache.put(protoKey, protoFile);
        assertFalse(metadataCache.containsKey(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX));

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
