/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.wildfly.clustering.server.service.Service;
import org.wildfly.clustering.singleton.SingletonStatus;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;

/**
 * @author Paul Ferraro
 */
public interface SingletonContext extends Service, SingletonStatus, SingletonElectionListener {
}
