/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.security;

import java.util.Map;
import java.util.Set;

import org.jboss.as.core.security.ServerSecurityManager;

/**
 * A simple transfer object
 *
 * @author anil saldhana
 */
class SecurityContextInterceptorHolder {
    ServerSecurityManager securityManager;
    String securityDomain, runAs, runAsPrincipal;
    String policyContextID;
    Set<String> extraRoles;
    Map<String, Set<String>> principalVsRolesMap;
    boolean skipAuthentication;

    public SecurityContextInterceptorHolder() {
    }

    public SecurityContextInterceptorHolder setSecurityManager(ServerSecurityManager ssm) {
        this.securityManager = ssm;
        return this;
    }

    public SecurityContextInterceptorHolder setSecurityDomain(String sd) {
        this.securityDomain = sd;
        return this;
    }

    public SecurityContextInterceptorHolder setRunAs(String ras) {
        this.runAs = ras;
        return this;
    }

    public SecurityContextInterceptorHolder setRunAsPrincipal(String ras) {
        this.runAsPrincipal = ras;
        return this;
    }

    public SecurityContextInterceptorHolder setPolicyContextID(String policyContextID) {
        this.policyContextID = policyContextID;
        return this;
    }

    public SecurityContextInterceptorHolder setExtraRoles(Set<String> er) {
        this.extraRoles = er;
        return this;
    }

    public SecurityContextInterceptorHolder setPrincipalVsRolesMap(Map<String, Set<String>> pr) {
        this.principalVsRolesMap = pr;
        return this;
    }

    public SecurityContextInterceptorHolder setSkipAuthentication(boolean skipAuthentication) {
        this.skipAuthentication = skipAuthentication;
        return this;

    }
}