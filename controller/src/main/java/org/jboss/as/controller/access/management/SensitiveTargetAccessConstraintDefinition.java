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

package org.jboss.as.controller.access.management;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.access.constraint.ConstraintFactory;
import org.jboss.as.controller.access.constraint.SensitiveTargetConstraint;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.dmr.ModelNode;

/**
 * {@link AccessConstraintDefinition} for {@link SensitiveTargetConstraint}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SensitiveTargetAccessConstraintDefinition implements AccessConstraintDefinition {

    public static final SensitiveTargetAccessConstraintDefinition ACCESS_CONTROL = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.ACCESS_CONTROL);
    public static final SensitiveTargetAccessConstraintDefinition CREDENTIAL = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.CREDENTIAL);
    public static final SensitiveTargetAccessConstraintDefinition DOMAIN_CONTROLLER = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.DOMAIN_CONTROLLER);
    public static final SensitiveTargetAccessConstraintDefinition DOMAIN_NAMES = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.DOMAIN_NAMES);
    public static final SensitiveTargetAccessConstraintDefinition EXTENSIONS = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.EXTENSIONS);
    public static final SensitiveTargetAccessConstraintDefinition JVM = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.JVM);
    public static final SensitiveTargetAccessConstraintDefinition MANAGEMENT_INTERFACES = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.MANAGEMENT_INTERFACES);
    public static final SensitiveTargetAccessConstraintDefinition MODULE_LOADING = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.MODULE_LOADING);
    public static final SensitiveTargetAccessConstraintDefinition PATCHING = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.PATCHING);
    public static final SensitiveTargetAccessConstraintDefinition READ_WHOLE_CONFIG = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.READ_WHOLE_CONFIG);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_DOMAIN = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SECURITY_DOMAIN);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_DOMAIN_REF = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SECURITY_DOMAIN_REF);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_REALM = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SECURITY_REALM);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_REALM_REF = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SECURITY_REALM_REF);
    public static final SensitiveTargetAccessConstraintDefinition SECURITY_VAULT = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SECURITY_VAULT);
    public static final SensitiveTargetAccessConstraintDefinition SERVICE_CONTAINER = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SERVICE_CONTAINER);
    public static final SensitiveTargetAccessConstraintDefinition SOCKET_BINDING_REF = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SOCKET_BINDING_REF);
    public static final SensitiveTargetAccessConstraintDefinition SOCKET_CONFIG = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SOCKET_CONFIG);
    public static final SensitiveTargetAccessConstraintDefinition SNAPSHOTS = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SNAPSHOTS);
    public static final SensitiveTargetAccessConstraintDefinition SYSTEM_PROPERTY = new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SYSTEM_PROPERTY);

    private final SensitivityClassification sensitivity;

    public SensitiveTargetAccessConstraintDefinition(SensitivityClassification sensitivity) {
        this.sensitivity = sensitivity;
        SensitiveTargetConstraint.FACTORY.addSensitivity(sensitivity);
    }

    public SensitivityClassification getSensitivity() {
        return sensitivity;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        ModelNode result = new ModelNode();
        result.get(TYPE).set(sensitivity.isCore() ? CORE : sensitivity.getSubsystem());
        return result;
    }

    @Override
    public ConstraintFactory getConstraintFactory() {
        return SensitiveTargetConstraint.FACTORY;
    }

    @Override
    public String getName() {
        return sensitivity.getName();
    }

    @Override
    public Type getType() {
        return Type.SENSITIVE;
    }

    public List<AccessConstraintDefinition> wrapAsList() {
        return Collections.singletonList((AccessConstraintDefinition) this);
    }
}
