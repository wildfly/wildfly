/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.timer;

import java.util.function.Function;

import org.jboss.modules.Module;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.eviction.EvictionConfiguration;

/**
 * Encapsulates the configuration of a {@link TimerManagementProvider}.
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface TimerManagementConfiguration extends EvictionConfiguration {

    Function<Module, ByteBufferMarshaller> getMarshallerFactory();

}
