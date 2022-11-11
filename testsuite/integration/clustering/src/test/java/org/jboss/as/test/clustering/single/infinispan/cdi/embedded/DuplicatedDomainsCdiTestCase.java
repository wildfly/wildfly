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

package org.jboss.as.test.clustering.single.infinispan.cdi.embedded;

import static org.junit.Assert.assertNotEquals;

import jakarta.inject.Inject;

import org.infinispan.AdvancedCache;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.cdi.embedded.deployment.CdiConfig;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests whether {@link DefaultCacheManager} sets custom Cache name to avoid JMX name collision.
 *
 * @author Radoslav Husar
 * @author Sebastian Laskawiec
 * @since 27
 */
@RunWith(Arquillian.class)
public class DuplicatedDomainsCdiTestCase {

    @Inject
    private AdvancedCache<Object, Object> greetingCache;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, DuplicatedDomainsCdiTestCase.class.getSimpleName() + ".war")
                .addClass(DuplicatedDomainsCdiTestCase.class)
                .addClass(CdiConfig.class)
                .add(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute(
                        "Dependencies", "org.infinispan, org.infinispan.commons, org.infinispan.cdi.common meta-inf, org.infinispan.cdi.embedded meta-inf"
                ).exportAsString()), "META-INF/MANIFEST.MF")
                ;
    }

    /**
     * Creates new {@link DefaultCacheManager}.
     * This test will fail if CDI Extension registers and won't set Cache Manager's name.
     */
    @Test
    public void testIfCreatingDefaultCacheManagerSucceeds() {
        greetingCache.put("test-key", "test-value");

        String cdiName = greetingCache.getCacheManager().getCacheManagerInfo().getName();

        DefaultCacheManager cacheManager = new DefaultCacheManager();
        String defaultName = cacheManager.getName();
        cacheManager.stop();

        assertNotEquals(defaultName, cdiName);
    }
}
