/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.security.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAP_GROUPS_TO_ROLES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * A builder to simplify the creation of operations to define new security realms.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityRealmAddBuilder {

    private boolean built = false;
    private final ModelNode realmAddress;
    private boolean mapGroupsToRoles = true;
    private AuthenticationBuilder authentication;

    private final List<ModelNode> additionalSteps = new ArrayList<ModelNode>();

    private SecurityRealmAddBuilder(final String name) {
        realmAddress = new ModelNode().add(CORE_SERVICE, MANAGEMENT).add(SECURITY_REALM, name);
    }

    public static SecurityRealmAddBuilder builder(final String name) {
        return new SecurityRealmAddBuilder(name);
    }

    public SecurityRealmAddBuilder setMapGroupsToRoles(final boolean mapGroupsToRoles) {
        assertNotBuilt();
        this.mapGroupsToRoles = mapGroupsToRoles;
        return this;
    }

    public AuthenticationBuilder authentication() {
        if (authentication == null) {
            authentication = new AuthenticationBuilder(this);
        }
        // May have wasted instantiating it but it will also check this is not built.
        authentication.assertNotBuilt();

        return authentication;
    }

    public ModelNode build() {
        assertNotBuilt();
        buildChildren();
        built = true;

        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();
        ModelNode steps = composite.get(STEPS);
        steps.add(getAddRealmOp());
        for (ModelNode step : additionalSteps) {
            steps.add(step);
        }

        return composite;
    }

    private void buildChildren() {
        if (authentication != null && authentication.isBuilt() == false) {
            authentication.build();
        }
    }

    private ModelNode getAddRealmOp() {
        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(realmAddress);
        add.get(OP).set(ADD);

        if (mapGroupsToRoles == false) {
            add.get(MAP_GROUPS_TO_ROLES).set(false);
        }

        return add;
    }

    ModelNode getRealmAddress() {
        return realmAddress.clone();
    }

    SecurityRealmAddBuilder addStep(final ModelNode step) {
        assertNotBuilt();
        additionalSteps.add(step);
        return this;
    }

    void assertNotBuilt() {
        if (built) {
            throw new IllegalStateException("Alreadt built.");
        }
    }

}
