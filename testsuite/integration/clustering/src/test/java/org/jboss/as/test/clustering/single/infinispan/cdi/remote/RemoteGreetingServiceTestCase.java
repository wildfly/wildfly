/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.cdi.remote;

import static org.junit.Assert.assertEquals;

import jakarta.inject.Inject;

import org.infinispan.client.hotrod.DataFormat;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.dataconversion.MediaType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.cdi.remote.deployment.RemoteGreetingCache;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case to verify @Remote CDI injection.
 *
 * @author Radoslav Husar
 * @since 27
 */
@RunWith(Arquillian.class)
public class RemoteGreetingServiceTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, RemoteGreetingServiceTestCase.class.getSimpleName() + ".war")
                .addClass(RemoteGreetingServiceTestCase.class)
                .addPackage(RemoteGreetingCache.class.getPackage())
                .add(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute(
                        "Dependencies", "org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.cdi.common meta-inf, org.infinispan.cdi.remote meta-inf, org.infinispan.protostream"
                ).exportAsString()), "META-INF/MANIFEST.MF")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                ;
    }

    private static final DataFormat DATA_FORMAT = DataFormat.builder().keyType(MediaType.TEXT_PLAIN).valueType(MediaType.TEXT_PLAIN).build();

    @Inject
    @RemoteGreetingCache
    private RemoteCache<String, String> greetingCacheWithQualifier;

    @Test
    public void test() {
        // A 'CLEAR' operation would fail if a cache not configured from our @Producer were to be injected
        // e.g. SecurityException: ISPN006017: Operation 'CLEAR' requires authentication
        greetingCacheWithQualifier.clear();

        assertEquals(0, greetingCacheWithQualifier.size());

        // The name set with @Remote annotation on @RemoteGreetingCache
        assertEquals("default", greetingCacheWithQualifier.getName());

        try {
            greetingCacheWithQualifier.withDataFormat(DATA_FORMAT).put("Hello", this.getClass().getName());

            assertEquals(1, greetingCacheWithQualifier.size());
        } finally {
            greetingCacheWithQualifier.clear();
        }
    }
}
