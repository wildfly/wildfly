/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.wildfly.clustering.server.dispatcher.Command;
import org.wildfly.clustering.singleton.SingletonStatus;

/**
 * @author Paul Ferraro
 */
public enum PrimaryProviderCommand implements Command<Boolean, SingletonStatus, RuntimeException> {
    INSTANCE;

    @Override
    public Boolean execute(SingletonStatus status) {
        return status.isPrimaryProvider();
    }
}
