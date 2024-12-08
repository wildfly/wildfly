/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * @author Paul Ferraro
 */
public interface UnaryComponentServiceDescriptor<T> extends UnaryServiceDescriptor<T>, ComponentServiceDescriptor<T> {
}
