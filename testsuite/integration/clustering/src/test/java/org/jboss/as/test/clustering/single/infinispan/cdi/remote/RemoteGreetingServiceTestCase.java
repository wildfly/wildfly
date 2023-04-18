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

package org.jboss.as.test.clustering.single.infinispan.cdi.remote;

import static org.junit.Assert.assertEquals;

import jakarta.inject.Inject;

import org.infinispan.client.hotrod.RemoteCache;
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
                        "Dependencies", "org.infinispan.commons, org.infinispan.client.hotrod, org.infinispan.cdi.common meta-inf, org.infinispan.cdi.remote meta-inf"
                ).exportAsString()), "META-INF/MANIFEST.MF")
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                ;
    }

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
        assertEquals("transactional", greetingCacheWithQualifier.getName());

        greetingCacheWithQualifier.put("Hello", this.getClass().getName());

        assertEquals(1, greetingCacheWithQualifier.size());
    }
}
