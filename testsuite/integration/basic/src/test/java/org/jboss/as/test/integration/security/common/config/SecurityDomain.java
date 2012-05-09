/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.common.config;

/**
 * Simple property holder for security domain configuration.
 * 
 * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask
 * @author Josef Cacek
 */
public class SecurityDomain {

    public static final String DEFAULT_NAME = "test-security-domain";

    private final String name;
    private final SecurityModule[] loginModules;
    private final SecurityModule[] authorizationModules;
    private final SecurityModule[] mappingModules;
    private final JaspiAuthn jaspiAuthn;
    private final JSSE jsse;

    // Constructors ----------------------------------------------------------

    private SecurityDomain(Builder builder) {
        this.name = builder.name != null ? builder.name : DEFAULT_NAME;
        this.loginModules = builder.loginModules;
        this.authorizationModules = builder.authorizationModules;
        this.mappingModules = builder.mappingModules;
        this.jaspiAuthn = builder.jaspiAuthn;
        this.jsse = builder.jsse;
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the name.
     * 
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the loginModules.
     * 
     * @return the loginModules.
     */
    public SecurityModule[] getLoginModules() {
        return loginModules;
    }

    /**
     * Get the authorizationModules.
     * 
     * @return the authorizationModules.
     */
    public SecurityModule[] getAuthorizationModules() {
        return authorizationModules;
    }

    /**
     * Get the mappingModules.
     * 
     * @return the mappingModules.
     */
    public SecurityModule[] getMappingModules() {
        return mappingModules;
    }

    /**
     * Get the jaspiAuthn.
     * 
     * @return the jaspiAuthn.
     */
    public JaspiAuthn getJaspiAuthn() {
        return jaspiAuthn;
    }

    /**
     * Get the jsse.
     * 
     * @return the jsse.
     */
    public JSSE getJsse() {
        return jsse;
    }
    // Embedded classes ------------------------------------------------------

    public static class Builder {

        private String name;
        private SecurityModule[] loginModules;
        private SecurityModule[] authorizationModules;
        private SecurityModule[] mappingModules;
        private JaspiAuthn jaspiAuthn;
        private JSSE jsse;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder loginModules(SecurityModule... loginModules) {
            this.loginModules = loginModules;
            return this;
        }

        public Builder authorizationModules(SecurityModule... authorizationModules) {
            this.authorizationModules = authorizationModules;
            return this;
        }

        public Builder mappingModules(SecurityModule... mappingModules) {
            this.mappingModules = mappingModules;
            return this;
        }

        public Builder jaspiAuthn(JaspiAuthn jaspiAuthn) {
            this.jaspiAuthn = jaspiAuthn;
            return this;
        }

        public Builder jsse(JSSE jsse) {
            this.jsse = jsse;
            return this;
        }

        public SecurityDomain build() {
            return new SecurityDomain(this);
        }
    }

}
