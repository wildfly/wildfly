/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.TimeoutException;

/**
 * Implementation of {@link Incrementor} which always throws a standard remotable (known to a client classloader) exception
 * which is caused by an Infinispan {@link RuntimeException} {@link CacheException} {@link TimeoutException} which the client
 * typically cannot load the class for.
 *
 * @author Radoslav Husar
 */
@Stateful
@Remote(Incrementor.class)
public class InfinispanExceptionThrowingIncrementorBean implements Incrementor {

    @Override
    public Result<Integer> increment() {
        throw new IllegalStateException("standard remotable exception that is caused by non-remotable infinispan CacheException", new TimeoutException("the non-remotable infinispan cause"));
    }
}
