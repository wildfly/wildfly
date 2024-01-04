/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 *
 * @author Jan Tymel
 */
public class SyslogAuditLog extends AbstractConfigurableElement {

    private final String format;
    private final String hostName;
    private final Integer port;
    private final String serverAddress;
    private final String sslContext;
    private final String transport;
    private final Integer maxReconnectAttempts;

    private SyslogAuditLog(Builder builder) {
        super(builder);
        this.format = builder.format;
        this.hostName = builder.hostName;
        this.port = builder.port;
        this.serverAddress = builder.serverAddress;
        this.sslContext = builder.sslContext;
        this.transport = builder.transport;
        this.maxReconnectAttempts = builder.maxReconnectAttempts;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        StringBuilder command = new StringBuilder("/subsystem=elytron/syslog-audit-log=").append(name)
                .append(":add(");

        if (isNotBlank(format)) {
            command.append("format=\"").append(format).append("\", ");
        }
        if (isNotBlank(hostName)) {
            command.append("host-name=\"").append(hostName).append("\", ");
        }
        if (port != null) {
            command.append("port=").append(port).append(", ");
        }
        if (isNotBlank(serverAddress)) {
            command.append("server-address=\"").append(serverAddress).append("\", ");
        }
        if (isNotBlank(sslContext)) {
            command.append("ssl-context=\"").append(sslContext).append("\", ");
        }
        if (isNotBlank(transport)) {
            command.append("transport=\"").append(transport).append("\", ");
        }
        if (maxReconnectAttempts != null) {
            command.append("reconnect-attempts=").append(maxReconnectAttempts).append(", ");
        }

        command.append(")");

        cli.sendLine(command.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/syslog-audit-log=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link FileAuditLog}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link FileAuditLog}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {

        private String format;
        private String hostName;
        private Integer port;
        private String serverAddress;
        private String sslContext;
        private String transport;
        private Integer maxReconnectAttempts;

        private Builder() {
        }

        public Builder withServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withTransportProtocol(String transport) {
            this.transport = transport;
            return this;
        }

        public Builder withHostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        public Builder withFormat(String format) {
            this.format = format;
            return this;
        }

        public Builder withSslContext(String sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder setMaxReconnectAttempts(int maxReconnectAttempts) {
            this.maxReconnectAttempts = maxReconnectAttempts;
            return this;
        }

        public SyslogAuditLog build() {
            return new SyslogAuditLog(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
