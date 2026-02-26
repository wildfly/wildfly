/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron server-ssl-context configuration implementation.
 *
 * @author Josef Cacek
 */
public class SimpleServerSslContext extends AbstractConfigurableElement implements ServerSslContext {

    private final String keyManager;
    private final String trustManager;
    private final String securityDomain;
    private final String[] protocols;
    private final boolean needClientAuth;
    private final Boolean authenticationOptional;
    private final int responseTimeout;
    private final int cacheSize;
    private final int cacheLifeTime;
    private final String responderUri;
    private final boolean responderOverride;
    private final boolean ignoreExtension;
    private boolean ocspStaplingEnabled = false;

    private SimpleServerSslContext(Builder builder) {
        super(builder);
        this.keyManager = builder.keyManager;
        this.trustManager = builder.trustManager;
        this.securityDomain = builder.securityDomain;
        this.protocols = builder.protocols;
        this.needClientAuth = builder.needClientAuth;
        this.authenticationOptional = builder.authenticationOptional;
        this.ocspStaplingEnabled = builder.ocspStaplingEnabled;
        this.responseTimeout = builder.responseTimeout;
        this.cacheSize = builder.cacheSize;
        this.cacheLifeTime = builder.cacheLifeTime;
        this.responderUri = builder.responderUri;
        this.responderOverride = builder.responderOverride;
        this.ignoreExtension = builder.ignoreExtension;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/server-ssl-context=twoWaySSC:add(key-manager=twoWayKM,protocols=["TLSv1.2"],
        // trust-manager=twoWayTM,security-domain=test,need-client-auth=true)
        StringBuilder sb = new StringBuilder("/subsystem=elytron/server-ssl-context=").append(name).append(":add(");
        if (StringUtils.isNotBlank(keyManager)) {
            sb.append("key-manager=\"").append(keyManager).append("\", ");
        }
        if (protocols != null) {
            sb.append("protocols=[")
                    .append(Arrays.stream(protocols).map(s -> "\"" + s + "\"").collect(Collectors.joining(", "))).append("], ");
        }
        if (StringUtils.isNotBlank(trustManager)) {
            sb.append("trust-manager=\"").append(trustManager).append("\", ");
        }
        if (StringUtils.isNotBlank(securityDomain)) {
            sb.append("security-domain=\"").append(securityDomain).append("\", ");
        }
        if (authenticationOptional != null) {
            sb.append("authentication-optional=").append(authenticationOptional).append(", ");
        }
        if (ocspStaplingEnabled) {
            sb.append("ocsp-stapling={");
            if(responseTimeout > 0) {
                sb.append("response-timeout=").append(responseTimeout).append(", ");
            }
            if(cacheSize != 0) {
                sb.append("cache-size=").append(cacheSize).append(", ");
            }
            if(cacheLifeTime > 0) {
                sb.append("cache-life-time=").append(cacheLifeTime).append(", ");
            }
            if(StringUtils.isNotBlank(responderUri)) {
                sb.append("responder-uri=\"").append(responderUri).append("\", ");
            }
            if(responderOverride) {
                sb.append("responder-override=true, ");
            } else {
                sb.append("responder-override=false, ");
            }
            if(ignoreExtension) {
                sb.append("ignore-extension=true, ");
            } else {
                sb.append("ignore-extension=false, ");
            }
            sb.append("}, ");
        }
        sb.append("need-client-auth=").append(needClientAuth).append(")");
        cli.sendLine(sb.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/server-ssl-context=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link SimpleServerSslContext}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link SimpleServerSslContext}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String keyManager;
        private String trustManager;
        private String securityDomain;
        private String[] protocols;
        private boolean needClientAuth;
        private Boolean authenticationOptional;
        private int responseTimeout;
        private int cacheSize;
        private int cacheLifeTime;
        private String responderUri;
        private boolean responderOverride;
        private boolean ignoreExtension;
        private boolean ocspStaplingEnabled;

        private Builder() {
        }

        public Builder withKeyManagers(String keyManagers) {
            this.keyManager = keyManagers;
            return this;
        }

        public Builder withTrustManagers(String trustManagers) {
            this.trustManager = trustManagers;
            return this;
        }

        public Builder withSecurityDomain(String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        public Builder withProtocols(String... protocols) {
            this.protocols = protocols;
            return this;
        }

        public Builder withNeedClientAuth(boolean needClientAuth) {
            this.needClientAuth = needClientAuth;
            return this;
        }

        public Builder withAuthenticationOptional(boolean authenticationOptional) {
            this.authenticationOptional = authenticationOptional;
            return this;
        }

        public Builder withResponseTimeout(int responseTimeout) {
            this.responseTimeout = responseTimeout;
            return this;
        }

        public Builder withCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        public Builder withCacheLifeTime(int cacheLifeTime) {
            this.cacheLifeTime = cacheLifeTime;
            return this;
        }

        public Builder withResponderUri(String responderUri) {
            this.responderUri = responderUri;
            return this;
        }

        public Builder withResponderOverride(boolean responderOverride) {
            this.responderOverride = responderOverride;
            return this;
        }

        public Builder withIgnoreExtension(boolean ignoreExtension) {
            this.ignoreExtension = ignoreExtension;
            return this;
        }
        public Builder withOcspStaplingEnabled(boolean ocspStaplingEnabled) {
            this.ocspStaplingEnabled = ocspStaplingEnabled;
            return this;
        }

        public SimpleServerSslContext build() {
            return new SimpleServerSslContext(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
