/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.bean;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

/**
 * @author Paul Ferraro
 */
@ProtoSchema(includeClasses = { Key.class, Value.class }, service = false)
public interface InfinispanCacheSerializationContextInitializer extends SerializationContextInitializer {

}
