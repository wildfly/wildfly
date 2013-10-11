/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.access.management;

import java.util.Set;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.Authorizer;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.CustomAuthorizer;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.JmxAction;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRBACAuthorizer;
import org.jboss.as.controller.access.rbac.SuperUserRoleMapper;

/**
 * A {@link org.jboss.as.controller.access.Authorizer} that delegates to another. Used for initial boot to allow
 * an instance of this class to be provided to the {@code ModelController} but then have the
 * functional implementation swapped out when boot proceeds to the point where the user-configured
 * authorizer is available.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class DelegatingConfigurableAuthorizer implements JmxAuthorizer {

    private final WritableAuthorizerConfiguration writableAuthorizerConfiguration;
    private volatile Authorizer delegate;

    public DelegatingConfigurableAuthorizer() {
        this.writableAuthorizerConfiguration =
                new WritableAuthorizerConfiguration(StandardRBACAuthorizer.AUTHORIZER_DESCRIPTION);
        this.delegate = StandardRBACAuthorizer.create(writableAuthorizerConfiguration,
                new SuperUserRoleMapper(writableAuthorizerConfiguration));
    }

    public WritableAuthorizerConfiguration getWritableAuthorizerConfiguration() {
        return writableAuthorizerConfiguration;
    }

    public void setDelegate(Authorizer delegate) {
        assert delegate != null : "null delegate";
        Authorizer currentDelegate = this.delegate;
        if (delegate instanceof CustomAuthorizer) {
            AuthorizerDescription description = ((CustomAuthorizer) delegate).setAuthorizerConfiguration(writableAuthorizerConfiguration);
            writableAuthorizerConfiguration.setAuthorizerDescription(description);
        } else {
            writableAuthorizerConfiguration.setAuthorizerDescription(delegate.getDescription());
        }
        this.delegate = delegate;

        if (currentDelegate instanceof CustomAuthorizer) {
            ((CustomAuthorizer) currentDelegate).shutdown();
        } else if (currentDelegate instanceof StandardRBACAuthorizer) {
            ((StandardRBACAuthorizer) currentDelegate).shutdown();
        }
    }

    @Override
    public Set<String> getCallerRoles(Caller caller, Environment callEnvironment, Set<String> runAsRoles) {
        return delegate.getCallerRoles(caller, callEnvironment, runAsRoles);
    }

    @Override
    public AuthorizerDescription getDescription() {
        return delegate.getDescription();
    }

    @Override
    public AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetAttribute target) {
        return delegate.authorize(caller, callEnvironment, action, target);
    }

    @Override
    public AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetResource target) {
        return delegate.authorize(caller, callEnvironment, action, target);
    }

    public AuthorizationResult authorizeJmxOperation(Caller caller, Environment callEnvironment, JmxAction action) {
        return delegate.authorizeJmxOperation(caller, callEnvironment, action);
    }

    @Override
    public void setNonFacadeMBeansSensitive(boolean sensitive) {
        writableAuthorizerConfiguration.setNonFacadeMBeansSensitive(sensitive);
    }

    public void shutdown() {
        if (delegate instanceof CustomAuthorizer) {
            ((CustomAuthorizer) delegate).shutdown();
        } else if (delegate instanceof StandardRBACAuthorizer) {
            ((StandardRBACAuthorizer) delegate).shutdown();
        }
    }

}
