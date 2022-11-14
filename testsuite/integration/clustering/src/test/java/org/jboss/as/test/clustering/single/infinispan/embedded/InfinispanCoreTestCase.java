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

package org.jboss.as.test.clustering.single.infinispan.embedded;

import static org.junit.Assert.assertEquals;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Adopted and adapted from Infinispan testsuite.
 *
 * @author Radoslav Husar
 * @author Tristan Tarrant
 * @since 27
 */
@RunWith(Arquillian.class)
public class InfinispanCoreTestCase {

    private EmbeddedCacheManager cm;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, InfinispanCoreTestCase.class.getSimpleName() + ".war")
                .addClass(InfinispanCoreTestCase.class)
                .add(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan services").exportAsString()), "META-INF/MANIFEST.MF")
                ;
    }

    @After
    public void cleanUp() {
        if (cm != null)
            cm.stop();
    }

    @Test
    public void testCacheManager() {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();
        gcb.defaultCacheName("default");

        cm = new DefaultCacheManager(gcb.build(), new ConfigurationBuilder().build());
        Cache<String, String> cache = cm.getCache();
        cache.put("a", "a");
        assertEquals("a", cache.get("a"));
    }
}