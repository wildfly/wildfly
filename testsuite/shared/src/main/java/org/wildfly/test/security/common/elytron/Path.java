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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jboss.as.test.shared.CliUtils.escapePath;

/**
 * Helper class for adding "path" and "relative-to" attributes into CLI commands.
 *
 * @author Josef Cacek
 */
public class Path implements CliFragment {

    public static final Path EMPTY = Path.builder().build();

    private final String path;
    private final String relativeTo;

    private Path(Builder builder) {
        this.path = builder.path;
        this.relativeTo = builder.relativeTo;
    }

    public String getPath() {
        return path;
    }

    public String getRelativeTo() {
        return relativeTo;
    }

    /**
     * Generates part of CLI string in form "[path=..., [relative-to=..., ]"
     */
    @Override
    public String asString() {
        StringBuilder sb = new StringBuilder();
        if (isNotBlank(path)) {
            sb.append(String.format("path=\"%s\", ", escapePath(path)));
            if (isNotBlank(relativeTo)) {
                sb.append(String.format("relative-to=\"%s\"", relativeTo));
            }
        }
        return sb.toString();
    }

    /**
     * Creates builder to build {@link Path}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link Path}.
     */
    public static final class Builder {
        private String path;
        private String relativeTo;

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

        public Path build() {
            return new Path(this);
        }
    }

}
