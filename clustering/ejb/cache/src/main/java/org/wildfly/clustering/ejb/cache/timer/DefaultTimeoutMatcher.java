/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * The default timeout matcher.
 * @author Paul Ferraro
 */
public enum DefaultTimeoutMatcher implements Predicate<Method> {
    INSTANCE;

    @Override
    public boolean test(Method method) {
        return false;
    }
}
