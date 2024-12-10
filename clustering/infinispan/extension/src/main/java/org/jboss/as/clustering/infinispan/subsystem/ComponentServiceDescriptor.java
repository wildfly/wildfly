/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.wildfly.service.descriptor.ServiceDescriptor;

/**
 * @author Paul Ferraro
 *
 */
public interface ComponentServiceDescriptor<T> extends ServiceDescriptor<T>, ResourceRegistration {

    @Override
    default String getName() {
        PathElement path = this.getPathElement();
        Stream.Builder<String> stream = Stream.builder();
        stream.add(this.getParentServiceDescriptor().getName());
        stream.add(path.getKey());
        if (!path.isWildcard()) {
            stream.add(path.getValue());
        }
        return stream.build().collect(Collectors.joining("."));
    }

    ServiceDescriptor<?> getParentServiceDescriptor();
}
