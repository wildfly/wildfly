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

import java.io.IOException;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.query.data.Person;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Adopted and adapted from Infinispan testsuite.
 * This won't work as it relies on org.infinispan.jboss-marshalling which we aren't including as it's deprecated.
 *
 * @author Radoslav Husar
 * @since 27
 */
@RunWith(Arquillian.class)
@Ignore
public class EmbeddedCacheGenericMarshallerTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, EmbeddedCacheGenericMarshallerTestCase.class.getSimpleName() + ".war")
                .addClass(EmbeddedCacheGenericMarshallerTestCase.class)
                .addClass(Person.class)
                .addAsResource("infinispan-embedded.xml", "infinispan.xml")
                .add(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan").exportAsString()), "META-INF/MANIFEST.MF")
                ;
    }

    @Test
    public void testPut() throws IOException {
        try (EmbeddedCacheManager cm = new DefaultCacheManager("infinispan.xml")) {
            Cache<String, Person> cache = cm.getCache("test1");
            cache.put("p1", new Person("diego", 1));
            assertEquals("diego", cache.get("p1").getName());
        }
    }
}