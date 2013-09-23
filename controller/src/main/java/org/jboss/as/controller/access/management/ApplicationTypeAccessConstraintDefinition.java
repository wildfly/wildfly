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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.ApplicationTypeConstraint;
import org.jboss.as.controller.access.constraint.ConstraintFactory;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * {@link AccessConstraintDefinition} for {@link ApplicationTypeConstraint}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ApplicationTypeAccessConstraintDefinition implements AccessConstraintDefinition {

    public static final ApplicationTypeAccessConstraintDefinition DEPLOYMENT = new ApplicationTypeAccessConstraintDefinition(ApplicationTypeConfig.DEPLOYMENT);

    public static final List<AccessConstraintDefinition> DEPLOYMENT_AS_LIST = DEPLOYMENT.wrapAsList();

    private final ApplicationTypeConfig applicationTypeConfig;
    private final AccessConstraintKey key;

    public ApplicationTypeAccessConstraintDefinition(ApplicationTypeConfig applicationTypeConfig) {
        this.applicationTypeConfig = applicationTypeConfig;
        this.key = new AccessConstraintKey(ModelDescriptionConstants.APPLICATION_CLASSIFICATION, applicationTypeConfig.isCore(),
                applicationTypeConfig.getSubsystem(), applicationTypeConfig.getName());
        ApplicationTypeConstraint.FACTORY.addApplicationTypeConfig(applicationTypeConfig);
    }

    @Override
    public ModelNode getModelDescriptionDetails(Locale locale) {
        return null;
    }

    @Override
    public ConstraintFactory getConstraintFactory() {
        return ApplicationTypeConstraint.FACTORY;
    }

    public ApplicationTypeConfig getApplicationTypeConfig() {
        return applicationTypeConfig;
    }

    @Override
    public String getName() {
        return applicationTypeConfig.getName();
    }

    @Override
    public String getType() {
        return ModelDescriptionConstants.APPLICATION;
    }

    @Override
    public boolean isCore() {
        return applicationTypeConfig.isCore();
    }

    @Override
    public String getSubsystemName() {
        return applicationTypeConfig.isCore() ? null : applicationTypeConfig.getSubsystem();
    }

    @Override
    public AccessConstraintKey getKey() {
        return key;
    }

    @Override
    public String getDescription(Locale locale) {
        // TODO
        return null;
    }

    @Override
    public int hashCode() {
        return applicationTypeConfig.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ApplicationTypeAccessConstraintDefinition
                && applicationTypeConfig.equals(((ApplicationTypeAccessConstraintDefinition)obj).applicationTypeConfig);
    }

    public List<AccessConstraintDefinition> wrapAsList() {
        return Collections.singletonList((AccessConstraintDefinition) this);
    }
}
