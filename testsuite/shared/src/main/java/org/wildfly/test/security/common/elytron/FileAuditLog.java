/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.common.elytron;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Helper class for adding "file-audit-log" attributes into CLI commands.
 *
 * @author Jan Tymel
 */
public class FileAuditLog extends AbstractConfigurableElement {

    private final String format;
    private final Boolean paramSynchronized;
    private final String path;
    private final String relativeTo;
    private final String encoding;

    private FileAuditLog(Builder builder) {
        super(builder);
        this.format = builder.format;
        this.paramSynchronized = builder.paramSynchronized;
        this.path = builder.path;
        this.relativeTo = builder.relativeTo;
        this.encoding = builder.encoding;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        StringBuilder command = new StringBuilder("/subsystem=elytron/file-audit-log=").append(name)
                .append(":add(");

        if (isNotBlank(format)) {
            command.append("format=\"").append(format).append("\", ");
        }
        if (paramSynchronized != null) {
            command.append("synchronized=").append(paramSynchronized).append(", ");
        }
        if (isNotBlank(path)) {
            command.append("path=\"").append(path).append("\", ");
        }
        if (isNotBlank(relativeTo)) {
            command.append("relative-to=\"").append(relativeTo).append("\", ");
        }
        if (isNotBlank(encoding)) {
            command.append("encoding=\"").append(encoding).append("\", ");
        }

        command.append(")");

        cli.sendLine(command.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/file-audit-log=%s:remove()", name));
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

        private String path;
        private String relativeTo;
        private Boolean paramSynchronized;
        private String format;
        private String encoding;

        private Builder() {
        }

        public Builder withPath(String path) {
            this.path = path;
            return this;
        }

        public Builder withRelativeTo(String relativeTo) {
            this.relativeTo = relativeTo;
            return this;
        }

        public Builder withSynchronized(boolean paramSynchronized) {
            this.paramSynchronized = paramSynchronized;
            return this;
        }

        public Builder withFormat(String format) {
            this.format = format;
            return this;
        }

        public Builder withEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public FileAuditLog build() {
            return new FileAuditLog(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
