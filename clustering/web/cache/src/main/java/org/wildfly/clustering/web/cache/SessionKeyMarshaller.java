/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache;

import java.io.IOException;

import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Generic marshaller for cache keys containing session identifiers.
 * @author Paul Ferraro
 */
public class SessionKeyMarshaller<K extends Key<String>> extends FunctionalScalarMarshaller<K, String> {

    public SessionKeyMarshaller(Class<K> targetClass, ExceptionFunction<String, K, IOException> resolver) {
        super(targetClass, SessionIdentifierMarshaller.INSTANCE, Key::getId, resolver);
    }
}
