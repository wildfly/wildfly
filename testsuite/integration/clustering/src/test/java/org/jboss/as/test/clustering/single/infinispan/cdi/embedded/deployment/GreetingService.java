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
