/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * @author Jan Martiska
 */
public class SimplePermissionMapper extends AbstractConfigurableElement implements PermissionMapper {

    private final List<PermissionMapping> mappings;
    private final MappingMode mappingMode;

    private SimplePermissionMapper(Builder builder) {
        super(builder);
        this.mappings = builder.mappings;
        this.mappingMode = builder.mappingMode;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        final StringBuilder cliCommand = new StringBuilder();
        cliCommand.append("/subsystem=elytron/simple-permission-mapper=")
                .append(name)
                .append(":add(");
        if (mappingMode != null) {
            cliCommand.append("mapping-mode=").append(mappingMode.toString()).append(",");
        }
        cliCommand.append("permission-mappings=[");

        cliCommand.append(mappings
                .stream()
                .map(PermissionMapping::toCLIString)
                .collect(Collectors.joining(",")));

        cliCommand.append("]");
        cliCommand.append(")");

        cli.sendLine(cliCommand.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine("/subsystem=elytron/simple-permission-mapper=" + name + ":remove");
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link UndertowSslContext}. The name attribute refers to ssl-context capability name.
     */
    public static final class Builder
            extends AbstractConfigurableElement.Builder<Builder> {

        private MappingMode mappingMode;
        private List<PermissionMapping> mappings;

        public Builder() {
            mappings = new ArrayList<>();
        }

        @Override
        protected Builder self() {
            return this;
        }

        public SimplePermissionMapper build() {
            return new SimplePermissionMapper(this);
        }

        public Builder mappingMode(MappingMode mappingMode) {
            this.mappingMode = mappingMode;
            return this;
        }

        public Builder permissionMapping(PermissionMapping mapping) {
            this.mappings.add(mapping);
            return this;
        }

        public Builder permissionMappings(PermissionMapping... mappings) {
            this.mappings.addAll(Arrays.asList(mappings));
            return this;
        }

    }

    public static final class PermissionMapping {

        private final List<PermissionRef> permissions;
        private final List<String> principals;
        private final List<String> roles;

        private PermissionMapping(Builder builder) {
            this.permissions = builder.permissions;
            this.principals = builder.principals;
            this.roles = builder.roles;
        }

        public List<PermissionRef> getPermissions() {
            return permissions;
        }

        public List<String> getPrincipals() {
            return principals;
        }

        public List<String> getRoles() {
            return roles;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String toCLIString() {
            StringBuilder result = new StringBuilder();
            result.append("{permissions=[");
            result.append(permissions.stream()
                    .map(PermissionRef::toCLIString)
                    .collect(Collectors.joining(",")));
            result.append("]");
            if (principals.size() > 0) {
                result.append(",principals=[");
                result.append(StringUtils.join(principals, ","));
                result.append("]");
            }
            if (roles.size() > 0) {
                result.append(",roles=[");
                result.append(StringUtils.join(roles, ","));
                result.append("]");
            }
            result.append("}");
            return result.toString();
        }

        public static final class Builder {

            private List<PermissionRef> permissions;
            private List<String> principals;
            private List<String> roles;

            public Builder() {
                permissions = new ArrayList<>();
                principals = new ArrayList<>();
                roles = new ArrayList<>();
            }

            public Builder withPermissions(PermissionRef... permissions) {
                this.permissions.addAll(Arrays.asList(permissions));
                return this;
            }

            public Builder withPrincipals(String... principals) {
                this.principals.addAll(Arrays.asList(principals));
                return this;
            }

            public Builder withRoles(String... roles) {
                this.roles.addAll(Arrays.asList(roles));
                return this;
            }

            public PermissionMapping build() {
                return new PermissionMapping(this);
            }

        }
    }
}
