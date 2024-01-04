/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

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
public class FileSystemRealm extends AbstractUserAttributeValuesCapableElement implements SecurityRealm {

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
        for (UserWithAttributeValues user : getUsersWithAttributeValues()) {
            cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:add-identity(identity=%s)", name, user.getName()));
            cli.sendLine(
                    String.format("/subsystem=elytron/filesystem-realm=%s:set-password(identity=%s, clear={password=\"%s\"})",
                            name, user.getName(), user.getPassword()));
            if (!user.getValues().isEmpty()) {
                cli.sendLine(String.format(
                        "/subsystem=elytron/filesystem-realm=%s:add-identity-attribute(identity=%s, name=groups, value=[%s])", name,
                        user.getName(), String.join(",", user.getValues())));
            }
        }
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/filesystem-realm=%s:remove()", name));
        FileUtils.deleteQuietly(tempFolder);
    }

    public Path getPath() {
        return this.path;
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
    public static final class Builder extends AbstractUserAttributeValuesCapableElement.Builder<Builder> {
        private Path path;
        private Integer level;

        private Builder() {
        }

        /** @implNote if a {@link Path} is set, the folder will not be automatically deleted after the test case completes. */
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
