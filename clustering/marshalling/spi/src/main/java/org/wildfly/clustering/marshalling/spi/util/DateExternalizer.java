/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.util;

import java.util.Date;
import java.util.function.LongFunction;

import org.wildfly.clustering.marshalling.spi.LongExternalizer;

/**
 * Externalizers for {@link Date} implementations.
 * @author Paul Ferraro
 */
public class DateExternalizer<D extends Date> extends LongExternalizer<D> {

    public DateExternalizer(Class<D> targetClass, LongFunction<D> factory) {
        super(targetClass, factory, Date::getTime);
    }
}
