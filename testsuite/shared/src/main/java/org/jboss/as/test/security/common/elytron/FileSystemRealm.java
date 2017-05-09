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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.security.common.elytron;

import static org.jboss.as.test.integration.security.common.Utils.createTemporaryFolder;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Configuration for filesystem-realms Elytron resource.
 *
 * @author Josef Cacek
 */
public class FileSystemRealm extends AbstractUserRolesCapableElement implements SecurityRealm {

    private final Path path;
    private final Integer level;
    private final File tempFolder;

    private FileSystemRealm(Builder builder) {
        super(builder);
        if (builder.path != null) {
            tempFolder = null;
            this.path = builder.path;
        } else {
            try {
                tempFolder = createTemporaryFolder("ely-" + getName());
            } catch (IOException e) {
                throw new RuntimeException("Unable to create temporary folder", e);
            }
            this.path = Path.builder().withPath(tempFolder.getAbsolutePath()).build();
        }
        level = builder.level;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        final String levelStr = level == null ? "" : ("level=" + level);
        cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add(%s, %s)", name, path.asString(), levelStr));
        for (UserWithRoles user : getUsersWithRoles()) {
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s/identity=%s:add()", name, user.getName()));
            cli.sendLine(
                    String.format("/subsystem=elytron/filesystem-realm=%s/identity=%s:set-password(clear={password=\"%s\"})",
                            name, user.getName(), user.getPassword()));
            cli.sendLine(
                    String.format("/subsystem=elytron/filesystem-realm=%s/identity=%s:add-attribute(name=groups, value=[%s])",
                            name, user.getName(), String.join(",", user.getRoles())));
        }
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:remove()", name));
        FileUtils.deleteQuietly(tempFolder);
    }

    /**
     * Creates builder to build {@link FileSystemRealm}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link FileSystemRealm}.
     */
    public static final class Builder extends AbstractUserRolesCapableElement.Builder<Builder> {
        private Path path;
        private Integer level;

        private Builder() {
        }

        public Builder withPath(Path path) {
            this.path = path;
            return this;
        }

        public Builder withLevel(Integer level) {
            this.level = level;
            return this;
        }

        public FileSystemRealm build() {
            return new FileSystemRealm(this);
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
