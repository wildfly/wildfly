/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.security;

import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.LifecycleInterceptor;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.metadata.javaee.jboss.RunAsIdentityMetaData;
import org.jboss.security.RunAs;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Map;

public class RunAsLifecycleInterceptor implements LifecycleInterceptor {

    private final Map<String, RunAsIdentityMetaData> runAsIdentityMetaDataMap;

    public RunAsLifecycleInterceptor(final Map<String, RunAsIdentityMetaData> runAsIdentityMetaDataMap) {
        this.runAsIdentityMetaDataMap = runAsIdentityMetaDataMap;
    }

    private void handle(ServletInfo servletInfo, LifecycleContext context) throws ServletException {
        RunAsIdentityMetaData identity = null;
        RunAs old = null;
        SecurityContext sc = SecurityActions.getSecurityContext();
        if (sc == null) {
            context.proceed();
            return;
        }
        try {
            identity = runAsIdentityMetaDataMap.get(servletInfo.getName());
            RunAsIdentity runAsIdentity = null;
            if (identity != null) {
                UndertowLogger.ROOT_LOGGER.tracef("%s, runAs: %s", servletInfo.getName(), identity);
                runAsIdentity = new RunAsIdentity(identity.getRoleName(), identity.getPrincipalName(), identity.getRunAsRoles());
            }
            old = SecurityActions.setRunAsIdentity(runAsIdentity, sc);

            // Perform the request
            context.proceed();
        } finally {
            if (identity != null) {
                SecurityActions.setRunAsIdentity(old, sc);
            }
        }
    }

    @Override
    public void init(ServletInfo servletInfo, Servlet servlet, LifecycleContext context) throws ServletException {
        if (servletInfo.getRunAs() != null) {
            handle(servletInfo, context);
        } else {
            context.proceed();
        }
    }

    @Override
    public void init(FilterInfo filterInfo, Filter filter, LifecycleContext context) throws ServletException {
        context.proceed();
    }

    @Override
    public void destroy(ServletInfo servletInfo, Servlet servlet, LifecycleContext context) throws ServletException {
        handle(servletInfo, context);
    }

    @Override
    public void destroy(FilterInfo filterInfo, Filter filter, LifecycleContext context) throws ServletException {
        context.proceed();
    }
}
