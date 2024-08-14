/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import java.util.Map;

import org.jgroups.stack.Protocol;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;

/**
 * A repository of protocol properties.
 * @author Paul Ferraro
 */
public interface ProtocolPropertiesRepository {
    NullaryServiceDescriptor<ProtocolPropertiesRepository> SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.clustering.jgroups.protocol-properties", ProtocolPropertiesRepository.class);

    Map<String, String> getProperties(Class<? extends Protocol> protocolClass);
}
