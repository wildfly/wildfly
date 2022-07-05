/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.jmx.rbac.deployment;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

import java.io.IOException;

/**
 * A {@link Filter} implementation that will always runAs the current {@link SecurityIdentity} to activate
 * any outflow identities.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@WebFilter(urlPatterns = { "/*" })
public class IdentityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        SecurityIdentity identity = SecurityDomain.getCurrent().getCurrentSecurityIdentity();
        try {
            identity.runAs(() -> {
                try {
                    chain.doFilter(request, response);
                } catch (IOException | ServletException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServletException) {
                throw (ServletException) cause;
            } else if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw e;
        }
    }

    @Override
    public void destroy() {
    }

}
