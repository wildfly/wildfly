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
package org.jboss.as.domain.management.access;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintUtilizationRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.domain.management._private.DomainManagementResolver;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SensitivityClassificationTypeResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_ELEMENT = PathElement.pathElement(TYPE);

    SensitivityClassificationTypeResourceDefinition() {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-control.constraint.sensitivity-classification-type"));
    }

    static ResourceEntry createResource(Map<String, SensitivityClassification> classificationsByType,
                                        String type, String name, AccessConstraintUtilizationRegistry registry) {
        return new SensitivityClassificationResource(PathElement.pathElement(type, name), classificationsByType, registry);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(SensitivityResourceDefinition.createSensitivityClassification());
    }

    private static class SensitivityClassificationResource extends AbstractClassificationResource {
        private static final Set<String> CHILD_TYPES = Collections.singleton(CLASSIFICATION);
        private final Map<String, SensitivityClassification> classificationsByName;
        private final AccessConstraintUtilizationRegistry registry;

        SensitivityClassificationResource(PathElement pathElement, Map<String,
                SensitivityClassification> classificationsByType,
                                          AccessConstraintUtilizationRegistry registry) {
            super(pathElement);
            this.classificationsByName = classificationsByType;
            this.registry = registry;
        }

        @Override
        public Set<String> getChildTypes() {
            return CHILD_TYPES;
        }


        @Override
        ResourceEntry getChildEntry(String type, String name) {
            if (type.equals(CLASSIFICATION)) {
                SensitivityClassification classification = classificationsByName.get(name);
                if (classification != null) {
                    String classificationType = getPathElement().getValue();
                    return SensitivityResourceDefinition.createSensitivityClassificationResource(classification,
                            classificationType, name, registry);
                }
            }
            return null;
        }

        @Override
        public Set<String> getChildrenNames(String type) {
            if (type.equals(CLASSIFICATION)) {
                return classificationsByName.keySet();
            }
            return Collections.emptySet();
        }

        @Override
        public Set<ResourceEntry> getChildren(String childType) {
            if (childType.equals(CLASSIFICATION)) {
                Set<ResourceEntry> entries = new HashSet<ResourceEntry>();
                String classificationType = getPathElement().getValue();
                for (Map.Entry<String, SensitivityClassification> entry : classificationsByName.entrySet()) {
                    entries.add(SensitivityResourceDefinition.createSensitivityClassificationResource(entry.getValue(),
                            classificationType, entry.getKey(), registry));
                }
                return entries;
            }
            return Collections.emptySet();
        }

    }
}
