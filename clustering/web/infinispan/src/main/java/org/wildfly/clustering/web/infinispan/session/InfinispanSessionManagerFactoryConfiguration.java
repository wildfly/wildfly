/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;
import org.wildfly.clustering.server.NodeFactory;
import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;

/**
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public interface InfinispanSessionManagerFactoryConfiguration<S, SC, AL, LC> extends SessionManagerFactoryConfiguration<S, SC, AL, LC>, InfinispanConfiguration {

    KeyAffinityServiceFactory getKeyAffinityServiceFactory();

    CommandDispatcherFactory getCommandDispatcherFactory();

    NodeFactory<Address> getMemberFactory();
}
