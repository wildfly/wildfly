/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import java.util.concurrent.Executor;

import org.jboss.as.clustering.service.SuspendableService;
import org.jboss.as.ejb3.timerservice.spi.ManagedTimerService;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;

/**
 * @author Paul Ferraro
 */
public class SuspendableTimerService extends DecoratedTimerService {

    public SuspendableTimerService(ManagedTimerService service, SuspendableActivityRegistry registry, Executor executor) {
        super(service, new SuspendableService(service, registry, executor));
    }
}
