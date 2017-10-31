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

import org.jboss.as.test.integration.management.util.CLIWrapper;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

    private FileAuditLog(Builder builder) {
        super(builder);
        this.format = builder.format;
        this.paramSynchronized = builder.paramSynchronized;
        this.path = builder.path;
        this.relativeTo = builder.relativeTo;
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

        public FileAuditLog build() {
            return new FileAuditLog(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
