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

import java.io.IOException;

import jakarta.annotation.Resource;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
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
 * Variant of the {@link HotRodClientTestCase} using container-managed objects.
 *
 * @author Radoslav Husar
 * @since 27
 */
@RunWith(Arquillian.class)
@ServerSetup({ ContainerManagedHotRodClientTestCase.ServerSetupTask.class })
public class ContainerManagedHotRodClientTestCase {


    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, ContainerManagedHotRodClientTestCase.class.getSimpleName() + ".war")
                .addClass(ContainerManagedHotRodClientTestCase.class)
                .addClass(Person.class)
                .addClass(PersonSerializationContextInitializer.class)
                .addClass(PersonSerializationContextInitializer.class.getName() + "Impl")
                .addAsServiceProvider(SerializationContextInitializer.class.getName(), PersonSerializationContextInitializer.class.getName() + "Impl")
                .add(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan, org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.query, org.infinispan.protostream").exportAsString()), "META-INF/MANIFEST.MF")
                ;
    }

    static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super("default", createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=infinispan-server:add(port=%d,host=%s)", INFINISPAN_SERVER_PORT, INFINISPAN_SERVER_ADDRESS)
                            .add("/subsystem=infinispan/remote-cache-container=query:add(default-remote-cluster=infinispan-server-cluster, tcp-keep-alive=true, marshaller=PROTOSTREAM, modules=[org.wildfly.clustering.web.hotrod], properties={infinispan.client.hotrod.auth_username=%s, infinispan.client.hotrod.auth_password=%s}, statistics-enabled=true)", INFINISPAN_APPLICATION_USER, INFINISPAN_APPLICATION_PASSWORD)
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

    @Resource(lookup = "java:jboss/infinispan/remote-container/query")
    private RemoteCacheContainer remoteCacheContainer;

    private RemoteCache<String, Object> remoteCache;

    private RemoteCache<String, Object> createRemoteCache() {
        RemoteCache<String, Object> remoteCache = remoteCacheContainer.getCache();
        remoteCache.clear();
        return remoteCache;
    }

    @Test
    public void testPutGetCustomObject() throws IOException {
        remoteCache = createRemoteCache();

        Person p = new Person("Martin");
        remoteCache.put("k1", p);
        assertEquals(p.getName(), ((Person) remoteCache.get("k1")).getName());
    }

}
