/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.single.infinispan.query.data;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * @author Radoslav Husar
 */
@AutoProtoSchemaBuilder(includeClasses = { Person.class }, service = false)
public interface PersonSchema extends GeneratedSchema {
}
