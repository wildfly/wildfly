/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.singleton.Singleton;

/**
 * @author Paul Ferraro
 */
public class PrimaryProviderCommand implements Command<Boolean, Singleton> {
    private static final long serialVersionUID = 3194143912789013072L;

    @Override
    public Boolean execute(Singleton singleton) {
        return singleton.isPrimary();
    }
}
