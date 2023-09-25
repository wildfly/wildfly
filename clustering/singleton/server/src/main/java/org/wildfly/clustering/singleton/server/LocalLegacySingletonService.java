/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.Service;
import org.wildfly.clustering.singleton.SingletonService;

/**
 * Local {@link SingletonService} implementation created using JBoss MSC 1.3.x service installation.
 * @author Paul Ferraro
 */
@Deprecated
public class LocalLegacySingletonService<T> extends LocalSingletonService implements SingletonService<T> {

    private final Service<T> service;

    public LocalLegacySingletonService(Service<T> service, LocalSingletonServiceContext context) {
        super(service, context);
        this.service = service;
    }

    @Override
    public T getValue() {
        return this.service.getValue();
    }
}
