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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.ApplicationTypeConstraint;
import org.jboss.as.controller.access.constraint.SensitiveTargetConstraint;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.constraint.VaultExpressionSensitivityConfig;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management._private.DomainManagementResolver;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RootAccessConstraintResourceDefinition extends SimpleResourceDefinition {

    public static PathElement PATH_ELEMENT = PathElement.pathElement(CORE_SERVICE, ACCESS_CONSTRAINT);

    private static final AccessConstraintResource RESOURCE = new AccessConstraintResource();
    private static volatile Map<String, Map<String, SensitivityClassification>> classifications;
    private static volatile Map<String, Map<String, ApplicationTypeConfig>> applicationTypes;

    private static Map<String, Map<String, SensitivityClassification>> getClassifications(){
        Collection<SensitivityClassification> current = SensitiveTargetConstraint.FACTORY.getSensitivities();
        if (classifications == null || classifications.size() != current.size()) {
            Map<String, Map<String, SensitivityClassification>> classificationsMap = new HashMap<String, Map<String,SensitivityClassification>>();
            for (SensitivityClassification classification : current) {
                Map<String, SensitivityClassification> byType = classificationsMap.get(classification.getName());
                if (byType == null) {
                    byType = new HashMap<String, SensitivityClassification>();
                    classificationsMap.put(classification.getName(), byType);
                }
                String type = classification.isCore() ? CORE : classification.getSubsystem();
                byType.put(type, classification);
            }
            classifications = classificationsMap;
        }
        return classifications;
    }

    private static Map<String, Map<String, ApplicationTypeConfig>> getApplicationTypes(){
        Collection<ApplicationTypeConfig> current = ApplicationTypeConstraint.FACTORY.getApplicationTypeConfigs();
        if (applicationTypes == null || applicationTypes.size() != current.size()) {
            Map<String, Map<String, ApplicationTypeConfig>> applicationTypeConfigMap = new HashMap<String, Map<String,ApplicationTypeConfig>>();
            for (ApplicationTypeConfig applicationType : current) {
                Map<String, ApplicationTypeConfig> byType = applicationTypeConfigMap.get(applicationType.getName());
                if (byType == null) {
                    byType = new HashMap<String, ApplicationTypeConfig>();
                    applicationTypeConfigMap.put(applicationType.getName(), byType);
                }
                String type = applicationType.isCore() ? CORE : applicationType.getSubsystem();
                byType.put(type, applicationType);
            }
            applicationTypes = applicationTypeConfigMap;
        }
        return applicationTypes;
    }

    public RootAccessConstraintResourceDefinition() {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-constraint"));
    }

    public static Resource getResource() {
        return RESOURCE;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(SensitivityResourceDefinition.createVaultExpressionConfiguration());
        resourceRegistration.registerSubModel(new SensitivityClassificationResourceDefinition());
        resourceRegistration.registerSubModel(new ApplicationTypeResourceDefinition());
    }

    private static class AccessConstraintResource extends AbstractClassificationResource {

        private static final Set<String> CHILD_TYPES;
        static {
            Set<String> set = new HashSet<>();
            set.add(SensitivityResourceDefinition.VAULT_ELEMENT.getKey());
            set.add(SensitivityClassificationResourceDefinition.PATH_ELEMENT.getKey());
            set.add(ApplicationTypeResourceDefinition.PATH_ELEMENT.getKey());
            CHILD_TYPES = Collections.unmodifiableSet(set);
        }

        public AccessConstraintResource() {
            super(PATH_ELEMENT);
        }

        @Override
        public Set<String> getChildTypes() {
            return CHILD_TYPES;
        }

        @Override
        ResourceEntry getChildEntry(String type, String name) {
            if (SensitivityResourceDefinition.VAULT_ELEMENT.getKey().equals(type) &&
                    SensitivityResourceDefinition.VAULT_ELEMENT.getValue().equals(name)) {
                return SensitivityResourceDefinition.createResource(VaultExpressionSensitivityConfig.INSTANCE, SensitivityResourceDefinition.VAULT_ELEMENT);

            } else if (SensitivityClassificationResourceDefinition.PATH_ELEMENT.getKey().equals(type)){
                Map<String, Map<String, SensitivityClassification>> classifications = getClassifications();
                Map<String, SensitivityClassification> classificationsByType = classifications.get(name);
                if (classificationsByType != null) {
                    return SensitivityClassificationResourceDefinition.createResource(classificationsByType, type, name);
                }
            } else if (ApplicationTypeResourceDefinition.PATH_ELEMENT.getKey().equals(type)) {
                Map<String, Map<String, ApplicationTypeConfig>> applicationTypes = getApplicationTypes();
                Map<String, ApplicationTypeConfig> applicationTypesByType = applicationTypes.get(name);
                if (applicationTypesByType != null) {
                    return ApplicationTypeResourceDefinition.createResource(applicationTypesByType, type, name);
                }
            }
            return null;
        }

        @Override
        public Set<String> getChildrenNames(String type) {
            if (SensitivityResourceDefinition.VAULT_ELEMENT.getKey().equals(type)) {
                return Collections.singleton(SensitivityResourceDefinition.VAULT_ELEMENT.getValue());
            } else if (SensitivityClassificationResourceDefinition.PATH_ELEMENT.getKey().equals(type)){
                Map<String, Map<String, SensitivityClassification>> classifications = getClassifications();
                return classifications.keySet();
            } else if (ApplicationTypeResourceDefinition.PATH_ELEMENT.getKey().equals(type)) {
                Map<String, Map<String, ApplicationTypeConfig>> configs = getApplicationTypes();
                return configs.keySet();
            }
            return Collections.emptySet();
        }

        @Override
        public Set<ResourceEntry> getChildren(String childType) {
            if (SensitivityResourceDefinition.VAULT_ELEMENT.getKey().equals(childType)) {
                return Collections.singleton(SensitivityResourceDefinition.createResource(VaultExpressionSensitivityConfig.INSTANCE, SensitivityResourceDefinition.VAULT_ELEMENT));
            } else if (SensitivityClassificationResourceDefinition.PATH_ELEMENT.getKey().equals(childType)){
                Map<String, Map<String, SensitivityClassification>> classifications = getClassifications();
                Set<ResourceEntry> children = new HashSet<ResourceEntry>();
                for (Map.Entry<String, Map<String, SensitivityClassification>> entry : classifications.entrySet()) {
                    children.add(SensitivityClassificationResourceDefinition.createResource(entry.getValue(), childType, entry.getKey()));
                }
                return children;
            } else if (ApplicationTypeResourceDefinition.PATH_ELEMENT.getKey().equals(childType)) {
                Map<String, Map<String, ApplicationTypeConfig>> applicationTypes = getApplicationTypes();
                Set<ResourceEntry> children = new HashSet<ResourceEntry>();
                for (Map.Entry<String, Map<String, ApplicationTypeConfig>> entry : applicationTypes.entrySet()) {
                    children.add(ApplicationTypeResourceDefinition.createResource(entry.getValue(), childType, entry.getKey()));
                }
                return children;
            }
            return Collections.emptySet();
        }
    }

}
