/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ee.concurrent;

import java.util.concurrent.Callable;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * Utilities for capturing the current SecurityIdentity and wrapping tasks.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class SecurityIdentityUtils {

    private SecurityIdentityUtils() {
    }

    static <T> Callable<T> doIdentityWrap(final Callable<T> callable) {
        final SecurityIdentity securityIdentity = getSecurityIdentity();
        return securityIdentity != null ? () -> securityIdentity.runAs(callable) : callable;
    }

    static Runnable doIdentityWrap(final Runnable runnable) {
        final SecurityIdentity securityIdentity = getSecurityIdentity();
        return securityIdentity != null ? () -> securityIdentity.runAs(runnable) : runnable;
    }

    private static SecurityIdentity getSecurityIdentity() {
        SecurityDomain securityDomain = SecurityDomain.getCurrent();
        return securityDomain != null ? securityDomain.getCurrentSecurityIdentity() : null;
    }
}
