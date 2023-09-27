/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
