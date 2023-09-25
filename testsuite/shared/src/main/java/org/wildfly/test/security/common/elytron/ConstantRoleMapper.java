/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.security.common.elytron;

import java.util.Objects;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * Configuration for constant-permission-mapper Elytron resource.
 *
 * @author Josef Cacek
 */
public class ConstantRoleMapper extends AbstractConfigurableElement implements RoleMapper {

    private static final String CONSTANT_ROLE_MAPPER = "constant-role-mapper";
    private static final PathAddress PATH_ELYTRON = PathAddress.pathAddress().append("subsystem", "elytron");

    private final String[] roles;

    private ConstantRoleMapper(Builder builder) {
        super(builder);
        this.roles = Objects.requireNonNull(builder.roles, "Roles must be provided");
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        ModelNode op = Util.createAddOperation(PATH_ELYTRON.append(CONSTANT_ROLE_MAPPER, name));
        ModelNode rolesNode = op.get("roles");
        for (String role : roles) {
            rolesNode.add(role);
        }
        Utils.applyUpdate(op, client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(Util.createRemoveOperation(PATH_ELYTRON.append(CONSTANT_ROLE_MAPPER, name)), client);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for this class.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {

        private String[] roles;

        private Builder() {
        }

        @Override
        protected Builder self() {
            return this;
        }

        public ConstantRoleMapper build() {
            return new ConstantRoleMapper(this);
        }

        public Builder withRoles(String... roles) {
            this.roles = roles;
            return this;
        }
    }
}
