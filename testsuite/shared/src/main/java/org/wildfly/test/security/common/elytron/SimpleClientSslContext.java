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
 * Elytron client-ssl-context configuration implementation.
 *
 * @author Jan Tymel
 */
public class SimpleClientSslContext extends AbstractConfigurableElement implements ClientSslContext {

    private final String keyManager;
    private final String trustManager;
    private final String[] protocols;

    private SimpleClientSslContext(Builder builder) {
        super(builder);
        this.keyManager = builder.keyManagers;
        this.trustManager = builder.trustManager;
        this.protocols = builder.protocols;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/client-ssl-context=twoWaySSC:add(key-manager=twoWayKM,protocols=["TLSv1.2"],
        // trust-manager=twoWayTM)
        StringBuilder sb = new StringBuilder("/subsystem=elytron/client-ssl-context=").append(name).append(":add(");
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
        cli.sendLine(sb.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/client-ssl-context=%s:remove()", name));
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
        private String keyManagers;
        private String trustManager;
        private String[] protocols;

        private Builder() {
        }

        public Builder withKeyManagers(String keyManagers) {
            this.keyManagers = keyManagers;
            return this;
        }

        public Builder withTrustManager(String trustManager) {
            this.trustManager = trustManager;
            return this;
        }

        public Builder withProtocols(String... protocols) {
            this.protocols = protocols;
            return this;
        }

        public SimpleClientSslContext build() {
            return new SimpleClientSslContext(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
