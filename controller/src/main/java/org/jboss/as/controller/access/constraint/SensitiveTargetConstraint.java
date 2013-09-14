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
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * {@link Constraint} related to whether a resource, attribute or operation is considered security sensitive.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SensitiveTargetConstraint extends AllowAllowNotConstraint {

    public static final SensitiveTargetConstraint.Factory FACTORY = new Factory();

    private static final SensitiveTargetConstraint SENSITIVE = new SensitiveTargetConstraint(true);
    private static final SensitiveTargetConstraint NOT_SENSITIVE = new SensitiveTargetConstraint(false);
    private static final SensitiveTargetConstraint ALLOWS = new SensitiveTargetConstraint(true, true);
    private static final SensitiveTargetConstraint DISALLOWS = new SensitiveTargetConstraint(false, true);

    private SensitiveTargetConstraint(boolean isSensitive) {
        super(isSensitive);
    }

    private SensitiveTargetConstraint(boolean allowsSensitive, boolean allowsNonSensitive) {
        super(allowsSensitive, allowsNonSensitive);
    }

    public static class Factory extends AbstractConstraintFactory {

        private final Map<SensitivityClassification.Key, SensitivityClassification> sensitivities =
                Collections.synchronizedMap(new HashMap<SensitivityClassification.Key, SensitivityClassification>());

        /** Singleton */
        private Factory() {
        }

        @Override
        public Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect) {
            if (role == StandardRole.ADMINISTRATOR
                    || role == StandardRole.SUPERUSER
                    || (role == StandardRole.AUDITOR
                            && actionEffect != Action.ActionEffect.WRITE_CONFIG
                            && actionEffect != Action.ActionEffect.WRITE_RUNTIME)) {
                return ALLOWS;
            }
            return DISALLOWS;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target) {
            return (isSensitiveAction(action, actionEffect) || isSensitiveAttribute(target, actionEffect)) ? SENSITIVE : NOT_SENSITIVE;
        }

        @Override
        public Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target) {
            return (isSensitiveAction(action, actionEffect) || isSensitiveResource(target, actionEffect)) ? SENSITIVE : NOT_SENSITIVE;
        }

        private boolean isSensitiveAction(Action action, Action.ActionEffect effect) {
            for (AccessConstraintDefinition constraintDefinition : action.getAccessConstraints()) {
                if (constraintDefinition instanceof SensitiveTargetAccessConstraintDefinition) {
                    SensitiveTargetAccessConstraintDefinition stcd = (SensitiveTargetAccessConstraintDefinition) constraintDefinition;
                    SensitivityClassification sensitivity = stcd.getSensitivity();
                    if (sensitivity.isSensitive(effect)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isSensitiveAttribute(TargetAttribute target, Action.ActionEffect effect) {
            for (AccessConstraintDefinition constraintDefinition : target.getAccessConstraints()) {
                if (constraintDefinition instanceof SensitiveTargetAccessConstraintDefinition) {
                    SensitiveTargetAccessConstraintDefinition stcd = (SensitiveTargetAccessConstraintDefinition) constraintDefinition;
                    SensitivityClassification sensitivity = stcd.getSensitivity();
                    if (sensitivity.isSensitive(effect)) {
                        return true;
                    }
                }
            }
            // Check the resource
            return isSensitiveResource(target.getTargetResource(), effect);
        }

        private boolean isSensitiveResource(TargetResource target, Action.ActionEffect effect) {
            for (AccessConstraintDefinition constraintDefinition : target.getAccessConstraints()) {
                if (constraintDefinition instanceof SensitiveTargetAccessConstraintDefinition) {
                    SensitiveTargetAccessConstraintDefinition stcd = (SensitiveTargetAccessConstraintDefinition) constraintDefinition;
                    SensitivityClassification sensitivity = stcd.getSensitivity();
                    if (sensitivity.isSensitive(effect)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public final void addSensitivity(SensitivityClassification sensitivity) {
            SensitivityClassification.Key key = sensitivity.getKey();
            SensitivityClassification existing = sensitivities.get(key);
            if (existing == null) {
                sensitivities.put(key, sensitivity);
            } else {
                // Check for programming error -- SensitivityClassification with same key created with
                // differing default settings
                assert existing.isCompatibleWith(sensitivity)
                        : "incompatible " + sensitivity.getClass().getSimpleName();
            }
        }

        public Collection<SensitivityClassification> getSensitivities(){
            return Collections.unmodifiableCollection(sensitivities.values());
        }

        @Override
        protected int internalCompare(AbstractConstraintFactory other) {
            // We have no preference
            return 0;
        }
    }
}
