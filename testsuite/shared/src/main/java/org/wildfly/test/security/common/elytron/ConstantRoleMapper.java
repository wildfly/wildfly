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
    public static final class Builder extends AbstractConfigurableElement.Builder<ConstantRoleMapper.Builder> {

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
