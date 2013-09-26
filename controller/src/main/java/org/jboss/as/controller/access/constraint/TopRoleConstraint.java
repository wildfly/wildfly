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
package org.jboss.as.controller.access.constraint;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.Action.ActionEffect;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * A {@link Constraint} for writing to the top level standard roles i.e. 'Auditor' and 'SuperUser'.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class TopRoleConstraint extends AllowAllowNotConstraint {

    private static final PathElement MGMT_ELEMENT = PathElement.pathElement(CORE_SERVICE, MANAGEMENT);
    private static final PathElement AUTHZ_ELEMENT = PathElement.pathElement(ACCESS, AUTHORIZATION);

    public static final ConstraintFactory FACTORY = new Factory();

    private static final TopRoleConstraint TOP_ROLE = new TopRoleConstraint(true);
    private static final TopRoleConstraint NOT_TOP_ROLE = new TopRoleConstraint(false);
    private static final TopRoleConstraint ALLOWS = new TopRoleConstraint(true, true);
    private static final TopRoleConstraint DISALLOWS = new TopRoleConstraint(false, true);

    private TopRoleConstraint(boolean isTopRole) {
        super(isTopRole);
    }

    private TopRoleConstraint(boolean allowsTopRole, boolean allowsNonTopRole) {
        super(allowsTopRole, allowsNonTopRole);
    }

    static class Factory extends AbstractConstraintFactory {

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            if (actionEffect != ActionEffect.WRITE_CONFIG && actionEffect != ActionEffect.WRITE_RUNTIME) {
                return ALLOWS;
            }
            return role == StandardRole.SUPERUSER ? ALLOWS : DISALLOWS;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return getRequiredConstraint(actionEffect, action, target.getTargetResource());
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return isTopRole(target) ? TOP_ROLE : NOT_TOP_ROLE;
        }

        private boolean isTopRole(TargetResource target) {
            PathAddress address = target.getResourceAddress();
            if (address.size() >= 3) {
                PathElement roleMapping;
                if (MGMT_ELEMENT.equals(address.getElement(0)) && AUTHZ_ELEMENT.equals(address.getElement(1))
                        && ROLE_MAPPING.equals((roleMapping = address.getElement(2)).getKey())) {
                    String roleName = roleMapping.getValue();
                    return StandardRole.AUDITOR.name().equalsIgnoreCase(roleName)
                            || StandardRole.SUPERUSER.name().equalsIgnoreCase(roleName);
                }
            }

            return false;
        }

        @Override
        protected int internalCompare(AbstractConstraintFactory other) {
            return 0;
        }
    }

}
