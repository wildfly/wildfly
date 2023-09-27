/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagerFactoryConfiguration;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.Group;

/**
 * @author Paul Ferraro
 */
public interface InfinispanTimerManagerFactoryConfiguration<I> extends TimerManagerFactoryConfiguration<I>, InfinispanConfiguration {

    ByteBufferMarshaller getMarshaller();
    KeyAffinityServiceFactory getKeyAffinityServiceFactory();
    CommandDispatcherFactory getCommandDispatcherFactory();
    Group<Address> getGroup();
}
