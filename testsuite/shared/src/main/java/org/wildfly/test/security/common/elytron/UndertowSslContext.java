/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Updates Undertow https-listener of the defaul-server to use given (Elytron server-ssl-context) SSL context
 * instead of SSL context from legacy security-realm.
 *
 * @author Ondrej Kotek
 */
public class UndertowSslContext extends AbstractConfigurableElement {

    private String httpsListener;

    private UndertowSslContext(Builder builder) {
        super(builder);
        this.httpsListener = builder.httpsListener;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        cli.sendLine("batch");
        cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:write-attribute" +
                "(name=ssl-context,value=%s)", httpsListener, name));
        cli.sendLine("run-batch");
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine("batch");
        cli.sendLine(String.format("/subsystem=undertow/server=default-server/https-listener=%s:write-attribute" +
                "(name=ssl-context,value=%s)", httpsListener, "applicationSSC"));
        cli.sendLine("run-batch");
    }

    /**
     * Creates builder to build {@link UndertowSslContext}. The name attribute refers to ssl-context capability name.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link UndertowSslContext}. The name attribute refers to ssl-context capability name.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String httpsListener = "https";

        private Builder() {
        }

        public Builder withHttpsListener(String httpsListener) {
            this.httpsListener = httpsListener;
            return this;
        }

        public UndertowSslContext build() {
            return new UndertowSslContext(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
