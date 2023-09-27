/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.cdi.embedded.deployment;

import jakarta.inject.Inject;

/**
 * This is the Greeting Service class. Each call to the {@link GreetingService#greet(String)} method will be cached in the greeting-cache (in this case
 * the CacheKey will be the name). If this method has been already called
 * with the same name the cached value will be returned and this method will not be called.
 * Adopted and adapted from Infinispan testsuite.
 *
 * @author Radoslav Husar
 * @author Kevin Pollet &lt;pollet.kevin@gmail.com&gt; (C) 2011
 * @since 27
 */
public class GreetingService {

    @Inject
    private GreetingCacheManager cacheManager;

    // JCache: @CacheResult(cacheName = "greeting-cache")
    public String greet(String name) {
        String greeting = "Hello " + name + " :)";
        cacheManager.cacheResult(name, greeting);
        return greeting;
    }

}
