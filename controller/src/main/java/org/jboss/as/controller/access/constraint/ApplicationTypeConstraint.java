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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.constraint.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.constraint.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * @link Constraint} related to whether a resource, attribute or operation is related to an application.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ApplicationTypeConstraint extends AllowAllowNotConstraint {

    public static final ApplicationTypeConstraint.Factory FACTORY = new Factory();

    private static final ApplicationTypeConstraint APPLICATION = new ApplicationTypeConstraint(true);
    private static final ApplicationTypeConstraint NON_APPLICATION = new ApplicationTypeConstraint(false);

    private ApplicationTypeConstraint(boolean isApplication) {
        super(ControlFlag.REQUIRED, isApplication);
    }

    private ApplicationTypeConstraint(boolean allowsApplication, boolean allowsNonApplication) {
        super(ControlFlag.REQUIRED, allowsApplication, allowsNonApplication);
    }

    @Override
    protected int internalCompare(AbstractConstraint other) {
        // We have no preference
        return 0;
    }

    public static class Factory implements ConstraintFactory {

        private final Map<ApplicationTypeConfig.Key, ApplicationTypeConfig> typeConfigs =
                Collections.synchronizedMap(new HashMap<ApplicationTypeConfig.Key, ApplicationTypeConfig>());

        /** Singleton */
        private Factory() {}

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            return new ApplicationTypeConstraint(true, role != StandardRole.DEPLOYER);
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return (isApplicationType(action) || isApplicationType(target.getTargetResource())) ? APPLICATION : NON_APPLICATION;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return (isApplicationType(action) || isApplicationType(target)) ? APPLICATION : NON_APPLICATION;
        }

        private boolean isApplicationType(Action action) {
            for (AccessConstraintDefinition constraintDefinition : action.getAccessConstraints()) {
                if (constraintDefinition instanceof ApplicationTypeAccessConstraintDefinition) {
                    ApplicationTypeAccessConstraintDefinition atcd = (ApplicationTypeAccessConstraintDefinition) constraintDefinition;
                    ApplicationTypeConfig atc = atcd.getApplicationTypeConfig();
                    if (atc.isApplicationType()) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isApplicationType(TargetResource target) {
            for (AccessConstraintDefinition constraintDefinition : target.getAccessConstraints()) {
                if (constraintDefinition instanceof ApplicationTypeAccessConstraintDefinition) {
                    ApplicationTypeAccessConstraintDefinition atcd = (ApplicationTypeAccessConstraintDefinition) constraintDefinition;
                    ApplicationTypeConfig atc = atcd.getApplicationTypeConfig();
                    if (atc.isApplicationType()) {
                        return true;
                    }
                }
            }
            return false;
        }

        public void addApplicationTypeConfig(ApplicationTypeConfig applicationTypeConfig) {
            ApplicationTypeConfig.Key key = applicationTypeConfig.getKey();
            ApplicationTypeConfig existing = typeConfigs.get(key);
            if (existing == null) {
                typeConfigs.put(key, applicationTypeConfig);
            } else if (!existing.isCompatibleWith(applicationTypeConfig)) {
                // Programming error -- ApplicationTypeConfigs with same key have been created with
                // differing default settings
                throw new IllegalStateException("incompatible ApplicationTypeConfig");
            }
        }


        public Collection<ApplicationTypeConfig> getApplicationTypeConfigs(){
            return Collections.unmodifiableCollection(typeConfigs.values());
        }

    }
}
