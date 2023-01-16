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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.single.infinispan.cdi.embedded.deployment.CdiConfig;
import org.jboss.as.test.clustering.single.infinispan.cdi.embedded.deployment.GreetingCache;
import org.jboss.as.test.clustering.single.infinispan.cdi.embedded.deployment.GreetingService;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Radoslav Husar
 * @author Kevin Pollet &lt;pollet.kevin@gmail.com&gt; (C) 2011
 * @since 27
 */
@RunWith(Arquillian.class)
public class GreetingServiceTestCase {

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap
                .create(WebArchive.class, GreetingServiceTestCase.class.getSimpleName() + ".war")
                .addPackage(CdiConfig.class.getPackage())
                .setManifest(new StringAsset(Descriptors.create(ManifestDescriptor.class).attribute("Dependencies", "org.infinispan, org.infinispan.commons, org.infinispan.cdi.common meta-inf, org.infinispan.cdi.embedded meta-inf").exportAsString()))
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml")
                ;
    }

    @Inject
    @GreetingCache
    private org.infinispan.Cache<String, String> greetingCache;

    @Inject
    private GreetingService greetingService;

    @Before
    public void init() {
        greetingCache.clear();
        assertEquals(0, greetingCache.size());
    }

    @Test
    public void testGreetMethod() {
        assertEquals("Hello Pete :)", greetingService.greet("Pete"));
    }

    @Test
    public void testGreetMethodCache() {
        greetingService.greet("Pete");

        assertEquals(1, greetingCache.size());
        assertTrue(greetingCache.containsValue("Hello Pete :)"));

        greetingService.greet("Manik");

        assertEquals(2, greetingCache.size());
        assertTrue(greetingCache.containsValue("Hello Manik :)"));

        greetingService.greet("Pete");

        assertEquals(2, greetingCache.size());
    }
}
