/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.SessionManagerFactory;

/**
 * @author Stuart Douglas
 */
public class ImmediateSessionManagerFactory implements SessionManagerFactory{

    private final SessionManager sessionManager;

    public ImmediateSessionManagerFactory(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public SessionManager createSessionManager(Deployment deployment) {
        return sessionManager;
    }
}
