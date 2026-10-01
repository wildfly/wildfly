/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.service.ServiceName;

import java.util.Map;
import java.util.Set;

/**
 * @author Eduardo Martins
 */
public class ConcurrentContextSetupAction implements SetupAction {

    private final ConcurrentContext concurrentContext;
    private final ServiceName serviceName;

    public ConcurrentContextSetupAction(ConcurrentContext concurrentContext, ServiceName serviceName) {
        this.concurrentContext = concurrentContext;
        this.serviceName = serviceName;
    }

    @Override
    public void setup(Map<String, Object> properties) {
        ConcurrentContext.pushCurrent(concurrentContext);
    }

    @Override
    public void teardown(Map<String, Object> properties) {
        ConcurrentContext.popCurrent();
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public Set<ServiceName> dependencies() {
        return Set.of(serviceName);
    }

    public ConcurrentContext getConcurrentContext() {
        return concurrentContext;
    }
}