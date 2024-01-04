/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.singleton.SingletonElectionListener;

/**
 * @author Paul Ferraro
 */
public interface SingletonServiceContext extends ServiceNameProvider {
    SingletonElectionListener getElectionListener();
}
