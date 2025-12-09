/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import java.util.function.Function;

import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.eviction.EvictionConfiguration;

/**
 * Encapsulates the configuration of a bean management provider.
 * @author Paul Ferraro
 */
public interface BeanManagementConfiguration extends EvictionConfiguration {

    /**
     * Returns a factory for creating a bean deployment's marshaller.
     * @return a marshaller factory
     */
    Function<BeanDeploymentMarshallingContext, ByteBufferMarshaller> getMarshallerFactory();
}
