/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.sso;

import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.infinispan.affinity.KeyAffinityServiceFactory;

/**
 * Configuration for an SSO manager factory.
 * @author Paul Ferraro
 */
public interface InfinispanSSOManagerFactoryConfiguration extends InfinispanConfiguration {

    KeyAffinityServiceFactory getKeyAffinityServiceFactory();
}
