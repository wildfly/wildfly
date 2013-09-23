/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Utility for adding access constraint descriptive metadata to other metadata.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AccessConstraintDescriptionProviderUtil {
    public static void addAccessConstraints(ModelNode result, List<AccessConstraintDefinition> accessConstraints, Locale locale) {
        if (accessConstraints.size() > 0) {
            ModelNode constraints = new ModelNode();
            for (AccessConstraintDefinition constraint : accessConstraints) {
                ModelNode constraintDesc = constraints.get(constraint.getType(), constraint.getName());
                constraintDesc.get(TYPE).set(constraint.isCore() ? CORE : constraint.getSubsystemName());
                String textDesc = constraint.getDescription(locale);
                if (textDesc != null) {
                    constraintDesc.get(DESCRIPTION).set(textDesc);
                }
                ModelNode details = constraint.getModelDescriptionDetails(locale);
                if (details != null && details.isDefined()) {
                    constraintDesc.get(ModelDescriptionConstants.DETAILS).set(details);
                }
            }
            result.get(ModelDescriptionConstants.ACCESS_CONSTRAINTS).set(constraints);
        }
    }

}
