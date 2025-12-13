/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query;

import static org.junit.Assert.assertEquals;

import jakarta.annotation.Resource;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheContainer;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
import org.jboss.as.test.clustering.single.infinispan.query.data.PersonSchema;
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
@ServerSetup({ ServerSetupTask.class })
public class ContainerManagedHotRodClientTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, ContainerManagedHotRodClientTestCase.class.getSimpleName() + ".war")
                .addClass(ContainerManagedHotRodClientTestCase.class)
                .addPackage(PersonSchema.class.getPackage())
                .addAsServiceProvider(SerializationContextInitializer.class.getName(), PersonSchema.class.getName() + "Impl")
                .setManifest(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.protostream").exportAsString()))
                ;
    }

    @Resource(lookup = "java:jboss/infinispan/remote-container/query")
    private RemoteCacheContainer remoteCacheContainer;

    @Test
    public void testPutGetCustomObject() {
        RemoteCache<String, Person> cache = this.remoteCacheContainer.getCache("query");
        cache.start();
        try {
            Person p = new Person("Martin");
            cache.put("k1", p);
            assertEquals(p.getName(), cache.get("k1").getName());
        } finally {
            cache.clear();
            cache.stop();
        }
    }
}
