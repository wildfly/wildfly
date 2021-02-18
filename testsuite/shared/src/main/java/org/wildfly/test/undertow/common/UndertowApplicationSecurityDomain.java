/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.undertow.common;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.AbstractConfigurableElement;
import org.wildfly.test.security.common.elytron.ConfigurableElement;

/**
 * A {@link ConfigurableElement} to add an application-security-domain resource to the Undertow subsystem.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
 public class UndertowApplicationSecurityDomain extends AbstractConfigurableElement {

     private final PathAddress address;
     private final String securityDomain;
     private final String httpAuthenticationFactory;
     private final boolean enableJacc;
     private final boolean enableJaspi;
     private final boolean integratedJaspi;

     UndertowApplicationSecurityDomain(Builder builder) {
         super(builder);
         this.address = PathAddress.pathAddress().append("subsystem", "undertow").append("application-security-domain", name);
         this.securityDomain = builder.securityDomain;
         this.httpAuthenticationFactory = builder.httpAuthenticationFactory;
         this.enableJacc = builder.enableJacc;
         this.enableJaspi = builder.enableJaspi;
         this.integratedJaspi = builder.integratedJaspi;
     }

     @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode add = Util.createAddOperation(address);
        setIfNotNull(add, "security-domain", securityDomain);
        setIfNotNull(add, "http-authentication-factory", httpAuthenticationFactory);
        if (enableJacc) {
            add.get("enable-jacc").set(enableJacc);
        }
        add.get("enable-jaspi").set(enableJaspi);
        add.get("integrated-jaspi").set(integratedJaspi);

        Utils.applyUpdate(add, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(address), client);
    }

    /**
      * Create a new Builder instance.
      *
      * @return a new Builder instance.
      */
     public static Builder builder() {
         return new Builder();
     }

     /**
      * Builder to build an application-security-domain resource in the Undertow subsystem
      */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {

        private String securityDomain;
        private String httpAuthenticationFactory;
        private boolean enableJacc = false;
        private boolean enableJaspi = true;
        private boolean integratedJaspi = true;

        /**
         * Set the security domain to be mapped from this application-security-domain.
         *
         * @param securityDomain the security domain to be mapped from this application-security-domain.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withSecurityDomain(final String securityDomain) {
            this.securityDomain = securityDomain;

            return this;
        }

        /**
         * Set the http-authentication-factory to be mapped from this application security-domain.
         *
         * @param httpAuthenticationFactory the http-authentication-factory to be mapped from this application security-domain.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder httpAuthenticationFactory(final String httpAuthenticationFactory) {
            this.httpAuthenticationFactory = httpAuthenticationFactory;

            return this;
        }

        /**
         * Set if Jakarta Authorization should be enabled for this application-security-domain.
         *
         * @param enableJacc if Jakarta Authorization should be enabled for this application-security-domain.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withEnableJacc(final boolean enableJacc) {
            this.enableJacc = enableJacc;

            return this;
        }

        /**
         * Set if JASPI should be enabled for this application-security-domain.
         *
         * @param enableJaspi if JASPI should be enabled for this application-security-domain.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withEnableJaspi(final boolean enableJaspi) {
            this.enableJaspi = enableJaspi;

            return this;
        }

        /**
         * Set if JASPI should operate in integrated mode or ad-hoc mode.
         *
         * @param integratedJaspi if JASPI should operate in integrated mode or ad-hoc mode.
         * @return this {@link Builder} to allow method chaining.
         */
        public Builder withIntegratedJaspi(final boolean integratedJaspi) {
            this.integratedJaspi = integratedJaspi;

            return this;
        }

        /**
         * Build a new UndertowApplicationSecurityDomain {@link ConfigurableElement}
         *
         * @return a new UndertowApplicationSecurityDomain {@link ConfigurableElement}
         */
        public UndertowApplicationSecurityDomain build() {
            return new UndertowApplicationSecurityDomain(this);
        }

        @Override
        protected Builder self() {
            return this;
        }

     }
}
