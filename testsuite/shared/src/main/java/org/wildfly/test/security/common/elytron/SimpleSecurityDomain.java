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

package org.wildfly.test.security.common.elytron;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * Elytron Security domain configuration.
 *
 * @author Josef Cacek
 */
public class SimpleSecurityDomain extends AbstractConfigurableElement implements SecurityDomain {

    private final String defaultRealm;
    private final Boolean outflowAnonymous;
    private final String[] outflowSecurityDomains;
    private final String permissionMapper;
    private final String preRealmPrincipalTransformer;
    private final String postRealmPrincipalTransformer;
    private final String principalDecoder;
    private final String realmMapper;
    private final SecurityDomainRealm[] realms;
    private final String roleMapper;
    private final String securityEventListener;
    private final String[] trustedSecurityDomains;

    private SimpleSecurityDomain(Builder builder) {
        super(builder);
        this.defaultRealm = builder.defaultRealm;
        this.outflowAnonymous = builder.outflowAnonymous;
        this.outflowSecurityDomains = builder.outflowSecurityDomains;
        this.permissionMapper = builder.permissionMapper;
        this.preRealmPrincipalTransformer = builder.preRealmPrincipalTransformer;
        this.postRealmPrincipalTransformer = builder.postRealmPrincipalTransformer;
        this.principalDecoder = builder.principalDecoder;
        this.realmMapper = builder.realmMapper;
        this.realms = builder.realms;
        this.roleMapper = builder.roleMapper;
        this.securityEventListener = builder.securityEventListener;
        this.trustedSecurityDomains = builder.trustedSecurityDomains;
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util
                .createAddOperation(PathAddress.pathAddress().append("subsystem", "elytron").append("security-domain", name));
        op.get("default-realm").set(defaultRealm);
        if (outflowAnonymous != null) {
            op.get("outflow-anonymous").set(outflowAnonymous);
        }
        setIfNotNull(op, "outflow-security-domains", outflowSecurityDomains);
        setIfNotNull(op, "permission-mapper", permissionMapper);
        setIfNotNull(op, "post-realm-principal-transformer", postRealmPrincipalTransformer);
        setIfNotNull(op, "pre-realm-principal-transformer", preRealmPrincipalTransformer);
        setIfNotNull(op, "principal-decoder", principalDecoder);
        setIfNotNull(op, "realm-mapper", realmMapper);
        if (realms != null) {
            ModelNode realmsNode = op.get("realms");
            for (SecurityDomainRealm realmRef : realms) {
                ModelNode realmRefNode = new ModelNode();
                realmRefNode.get("realm").set(realmRef.realm);
                setIfNotNull(realmRefNode, "principal-transformer", realmRef.principalTransformer);
                setIfNotNull(realmRefNode, "role-decoder", realmRef.roleDecoder);
                setIfNotNull(realmRefNode, "role-mapper", realmRef.roleMapper);
                realmsNode.add(realmRefNode);
            }
        }
        setIfNotNull(op, "role-mapper", roleMapper);
        setIfNotNull(op, "security-event-listener", securityEventListener);
        setIfNotNull(op, "trusted-security-domains", trustedSecurityDomains);
        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(
                PathAddress.pathAddress().append("subsystem", "elytron").append("security-domain", name)), client);
    }

    /**
     * Creates builder to build {@link SimpleSecurityDomain}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleSecurityDomain}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String defaultRealm;
        private Boolean outflowAnonymous;
        private String[] outflowSecurityDomains;
        private String permissionMapper;
        private String preRealmPrincipalTransformer;
        private String postRealmPrincipalTransformer;
        private String principalDecoder;
        private String realmMapper;
        private SecurityDomainRealm[] realms;
        private String roleMapper;
        private String securityEventListener;
        private String[] trustedSecurityDomains;

        private Builder() {
        }

        public Builder withDefaultRealm(String defaultRealm) {
            this.defaultRealm = defaultRealm;
            return this;
        }

        public Builder withOutflowAnonymous(Boolean outflowAnonymous) {
            this.outflowAnonymous = outflowAnonymous;
            return this;
        }

        public Builder withOutflowSecurityDomains(String... outflowSecurityDomains) {
            this.outflowSecurityDomains = outflowSecurityDomains;
            return this;
        }

        public Builder withPermissionMapper(String permissionMapper) {
            this.permissionMapper = permissionMapper;
            return this;
        }

        public Builder withPreRealmPrincipalTransformer(String preRealmPrincipalTransformer) {
            this.preRealmPrincipalTransformer = preRealmPrincipalTransformer;
            return this;
        }

        public Builder withPostRealmPrincipalTransformer(String postRealmPrincipalTransformer) {
            this.postRealmPrincipalTransformer = postRealmPrincipalTransformer;
            return this;
        }

        public Builder withPrincipalDecoder(String principalDecoder) {
            this.principalDecoder = principalDecoder;
            return this;
        }

        public Builder withRealmMapper(String realmMapper) {
            this.realmMapper = realmMapper;
            return this;
        }

        public Builder withRealms(SecurityDomainRealm... realms) {
            this.realms = realms;
            return this;
        }

        public Builder withRoleMapper(String roleMapper) {
            this.roleMapper = roleMapper;
            return this;
        }

        public Builder withSecurityEventListener(String securityEventListener) {
            this.securityEventListener = securityEventListener;
            return this;
        }

        public Builder withTrustedSecurityDomains(String[] trustedSecurityDomains) {
            this.trustedSecurityDomains = trustedSecurityDomains;
            return this;
        }

        public SimpleSecurityDomain build() {
            return new SimpleSecurityDomain(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    public static class SecurityDomainRealm {
        private final String realm;
        private final String principalTransformer;
        private final String roleDecoder;
        private final String roleMapper;

        private SecurityDomainRealm(Builder builder) {
            this.realm = builder.realm;
            this.principalTransformer = builder.principalTransformer;
            this.roleDecoder = builder.roleDecoder;
            this.roleMapper = builder.roleMapper;
        }

        /**
         * Creates builder to build {@link SecurityDomainRealm}.
         *
         * @return created builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder to build {@link SecurityDomainRealm}.
         */
        public static final class Builder {
            private String realm;
            private String principalTransformer;
            private String roleDecoder;
            private String roleMapper;

            private Builder() {
            }

            public Builder withRealm(String realm) {
                this.realm = realm;
                return this;
            }

            public Builder withPrincipalTransformer(String principalTransformer) {
                this.principalTransformer = principalTransformer;
                return this;
            }

            public Builder withRoleDecoder(String roleDecoder) {
                this.roleDecoder = roleDecoder;
                return this;
            }

            public Builder withRoleMapper(String roleMapper) {
                this.roleMapper = roleMapper;
                return this;
            }

            public SecurityDomainRealm build() {
                return new SecurityDomainRealm(this);
            }
        }
    }
}
