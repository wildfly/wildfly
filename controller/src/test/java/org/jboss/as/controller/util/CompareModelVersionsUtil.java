/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Compares the current running server with the passed in version. The passed in version
 * must have model and resource definition dumps in {@code src/test/resources/legacy-models} as
 * outlined in {@link DumpResourceDefinitionUtil} and {@link GrabModelVersionsUtil}.
 * <p>
 * To run this a big heap size is needed, e.g. -Xmx1024m
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CompareModelVersionsUtil {

    private final boolean compareDifferentVersions;
    private final String targetVersion;
    private final ModelNode legacyModelVersions;
    private final ModelNode legacyResourceDefinitions;
    private final ModelNode currentModelVersions;
    private final ModelNode currentResourceDefinitions;

    private CompareModelVersionsUtil(boolean compareDifferentVersions,
            String targetVersion,
            ModelNode legacyModelVersions,
            ModelNode legacyResourceDefinitions,
            ModelNode currentModelVersions,
            ModelNode currentResourceDefinitions) throws Exception {
        this.compareDifferentVersions = compareDifferentVersions;
        this.targetVersion = targetVersion;
        this.legacyModelVersions = legacyModelVersions;
        this.legacyResourceDefinitions = legacyResourceDefinitions;
        this.currentModelVersions = currentModelVersions;
        this.currentResourceDefinitions = currentResourceDefinitions;
    }

    public static void main(String[] args) throws Exception {

        String version = System.getProperty("jboss.as.compare.version", null);
        String diff = System.getProperty("jboss.as.compare.different.versions", null);
        String type = System.getProperty("jboss.as.comare.type", null);

        if (version == null) {
            System.out.print("Enter legacy AS version: ");
            version = readInput(null);
        }
        System.out.println("Using target model: " + version);

        if (type == null) {
            System.out.print("Enter type [S](standalone)/H(host)/D(domain):");
            type = readInput("S");
        }
        final ResourceType resourceType;
        if (ResourceType.STANDALONE.toString().startsWith(type.toUpperCase())) {
            resourceType = ResourceType.STANDALONE;
        } else if (ResourceType.HOST.toString().startsWith(type.toUpperCase())) {
            resourceType = ResourceType.HOST;
        }  else if (ResourceType.DOMAIN.toString().startsWith(type.toUpperCase())) {
            resourceType = ResourceType.DOMAIN;
        } else {
            throw new IllegalArgumentException("Could not determine type for: '" + type + "'");
        }



        if (diff == null) {
            System.out.print("Report on differences in the model when the management versions are different? y/[n]: ");
            diff = readInput("n").toLowerCase();
        }
        boolean compareDifferentVersions;
        if (diff.equals("n")) {
            System.out.println("Reporting on differences in the model when the management versions are different");
            compareDifferentVersions = false;
        } else if (diff.equals("y")) {
            System.out.println("Not reporting on differences in the model when the management versions are different");
            compareDifferentVersions = true;
        } else {
            throw new IllegalArgumentException("Please enter 'y' or 'n'");
        }

        System.out.println("Loading legacy model versions for " + version + "....");
        ModelNode legacyModelVersions = Tools.loadModelNodeFromFile(new File("target/test-classes/legacy-models/standalone-model-versions-" + version + ".dmr"));
        System.out.println("Loaded legacy model versions");

        System.out.println("Loading legacy resource descriptions for " + version + "....");
        ModelNode legacyResourceDefinitions = Tools.loadModelNodeFromFile(new File("target/test-classes/legacy-models/" + resourceType.toString().toLowerCase() + "-resource-definition-" + version + ".dmr"));
        System.out.println("Loaded legacy resource descriptions");

        System.out.println("Loading model versions for currently running server...");
        ModelNode currentModelVersions = Tools.getCurrentModelVersions();
        System.out.println("Loaded current model versions");

        System.out.println("Loading resource descriptions for currently running server...");
        final ModelNode currentResourceDefinitions;
        if (resourceType == ResourceType.STANDALONE) {
            currentResourceDefinitions = Tools.getCurrentRunningResourceDefinition(PathAddress.EMPTY_ADDRESS);
        } else if (resourceType == ResourceType.DOMAIN) {
            currentResourceDefinitions = Tools.getCurrentRunningDomainResourceDefinition();
        } else {
            currentResourceDefinitions = Tools.getCurrentRunningResourceDefinition(PathAddress.pathAddress(PathElement.pathElement(HOST, "master")));
        }
        System.out.println("Loaded current resource descriptions");

        CompareModelVersionsUtil compareModelVersionsUtil = new CompareModelVersionsUtil(compareDifferentVersions, version, legacyModelVersions, legacyResourceDefinitions, currentModelVersions, currentResourceDefinitions);

        System.out.println("Starting comparison of the current....\n");
        compareModelVersionsUtil.compareModels();
        System.out.println("\nDone comparison!");
    }


    private static String readInput(String defaultAnswer) throws IOException {
        StringBuilder sb = new StringBuilder();
        char c = (char)System.in.read();
        while (c != '\n') {
            sb.append(c);
            c = (char)System.in.read();
        }
        String s = sb.toString().trim();
        if (s.equals("")) {
            if (defaultAnswer != null) {
                return defaultAnswer;
            }
            throw new IllegalArgumentException("Please enter a valid answer");
        }
        return s;
    }

    private void compareModels() {
        compareCoreModels();
        compareSubsystemModels();
    }

    private void compareCoreModels() {
        System.out.println("====== Comparing core models ======");
        ResourceDefinition currentDefinition = new ResourceDefinition(trimSubsystem(currentResourceDefinitions), currentModelVersions);
        ResourceDefinition legacyDefinition = new ResourceDefinition(trimSubsystem(legacyResourceDefinitions), legacyModelVersions);
        CompareContext context = new CompareContext(PathAddress.EMPTY_ADDRESS, PathAddress.EMPTY_ADDRESS, true, currentDefinition, legacyDefinition);
        if (!context.continuteWithCheck()) {
            return;
        }
        compareModel(context);
    }

    private void compareSubsystemModels() {
        System.out.println("====== Comparing subsystem models ======");
        ResourceDefinition rootCurrentDefinition = new ResourceDefinition(trimNonSubsystem(currentResourceDefinitions), currentModelVersions);
        ResourceDefinition rootLegacyDefinition = new ResourceDefinition(trimNonSubsystem(legacyResourceDefinitions), legacyModelVersions);
        Map<String, ModelNode> currentSubsystems = rootCurrentDefinition.getChildren(SUBSYSTEM);
        Map<String, ModelNode> legacySubsystems = rootLegacyDefinition.getChildren(SUBSYSTEM);

        CompareContext context = new CompareContext(PathAddress.EMPTY_ADDRESS, PathAddress.EMPTY_ADDRESS, true, rootCurrentDefinition, rootLegacyDefinition);
        compareKeySetsAndRemoveMissing(context, "subsystems", currentSubsystems, legacySubsystems);

        for (Map.Entry<String, ModelNode> legacyEntry : legacySubsystems.entrySet()) {
            PathAddress subsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, legacyEntry.getKey()));
            ResourceDefinition currentDefinition = new ResourceDefinition(currentSubsystems.get(legacyEntry.getKey()), currentModelVersions);
            ResourceDefinition legacyDefinition = new ResourceDefinition(legacyEntry.getValue(), legacyModelVersions);
            context = new CompareContext(subsystemAddress, subsystemAddress, false, currentDefinition, legacyDefinition);
            if (!context.continuteWithCheck()) {
                continue;
            }
            compareModel(context);
        }
    }



    private ModelNode trimSubsystem(ModelNode definition) {
        ModelNode def = definition.clone();
        def.get(CHILDREN).remove(SUBSYSTEM);
        return def;
    }

    private ModelNode trimNonSubsystem(ModelNode definition) {
        ModelNode def = definition.clone();
        for (String key : def.get(CHILDREN).keys()) {
            if (!key.equals(SUBSYSTEM)) {
                def.remove(key);
            }
        }
        return def;
    }

    private void compareModel(CompareContext context) {
        //System.out.println("---->  " + context.getPathAddress());
        compareAttributes(context);
        compareOperations(context);
        compareChildren(context);
    }

    private void compareAttributes(CompareContext context) {
        Map<String, ModelNode> legacyAttributes = context.getLegacyDefinition().getAttributes();
        Map<String, ModelNode> currentAttributes = context.getCurrentDefinition().getAttributes();

        compareKeySetsAndRemoveMissing(context, "attributes", currentAttributes, legacyAttributes);
        //TODO compare types, expressions etc.

        for (Map.Entry<String, ModelNode> legacyEntry : legacyAttributes.entrySet()) {
            ModelNode legacyAttribute = legacyEntry.getValue();
            ModelNode currentAttribute = currentAttributes.get(legacyEntry.getKey());

            String id = "attribute '" + legacyEntry.getKey() + "'";
            compareAttributeOrOperationParameter(context, id, currentAttribute, legacyAttribute);
            compareAccessType(context, id, currentAttribute, legacyAttribute);
            compareStorage(context, id, currentAttribute, legacyAttribute);
        }
    }

    private void compareOperations(CompareContext context) {
        Map<String, ModelNode> legacyOperations = context.getLegacyDefinition().getOperations();
        Map<String, ModelNode> currentOperations = context.getCurrentDefinition().getOperations();

        compareKeySetsAndRemoveMissing(context, "operations", currentOperations, legacyOperations);

        for (Map.Entry<String, ModelNode> legacyOpEntry : legacyOperations.entrySet()) {
            String operationName = legacyOpEntry.getKey();
            ModelNode legacyOperation = legacyOpEntry.getValue();
            ModelNode currentOperation = currentOperations.get(operationName);

            Map<String, ModelNode> legacyParameters = context.getLegacyDefinition().getOperationParameters(operationName);
            Map<String, ModelNode> currentParameters = context.getCurrentDefinition().getOperationParameters(operationName);

            compareKeySetsAndRemoveMissing(context, "parameters for operation '" + operationName + "'", currentParameters, legacyParameters);

            for (Map.Entry<String, ModelNode> legacyParamEntry : legacyParameters.entrySet()) {
                ModelNode legacyParameter = legacyParamEntry.getValue();
                ModelNode currentParameter = currentParameters.get(legacyParamEntry.getKey());

                String id = "parameter '" + legacyParamEntry.getKey() + "' of operation '" + operationName + "'";
                compareAttributeOrOperationParameter(context, id, currentParameter, legacyParameter);
            }

            ModelNode legacyReply = legacyOperation.get(REPLY_PROPERTIES);
            ModelNode currentReply = currentOperation.get(REPLY_PROPERTIES);
            compareAttributeOrOperationParameter(context, "'reply-properties' for operation '" + operationName + "'", currentReply, legacyReply);
//            if (!currentReply.equals(legacyReply)) {
//                context.println("Different 'reply-properties' for operation '" + operationName + "'. Current: " + currentReply + "; legacy: " + legacyReply);
//            }
        }
    }

    private void compareAttributeOrOperationParameter(CompareContext context, String id, ModelNode current, ModelNode legacy) {
        compareType(context, id, current, legacy);
        compareValueType(context, id, current, legacy);
        compareNillable(context, id, current, legacy);
        compareExpressionsAllowed(context, id, current, legacy);
        //TODO compare anything else?
    }

    private void compareType(CompareContext context, String id, ModelNode current, ModelNode legacy) {
        if (!current.get(TYPE).equals(legacy.get(TYPE))) {
            context.println("Different 'type' for " + id + ". Current: " + current.get(TYPE) + "; legacy: " + legacy.get(TYPE));
        }
    }

    private void compareValueType(CompareContext context, String id, ModelNode current, ModelNode legacy) {
        ModelNode currentValueType = current.get(VALUE_TYPE);
        ModelNode legacyValueType = legacy.get(VALUE_TYPE);
        if (!currentValueType.isDefined() && !legacyValueType.isDefined()) {
            return;
        }
        if (isType(legacyValueType) || isType(currentValueType)) {
            if (!currentValueType.equals(legacyValueType)) {
                context.println("Different 'value-type' for " + id + ". Current: " + current.get(VALUE_TYPE) + "; legacy: " + legacy.get(VALUE_TYPE));
            }
        } else {
            Map<String, ModelNode> legacyValueTypes =  createMapIndexedByKey(legacyValueType);
            Map<String, ModelNode> currentValueTypes = createMapIndexedByKey(currentValueType);

            compareKeySetsAndRemoveMissing(context, "value-type for " + id, currentValueTypes, legacyValueTypes);
            for (Map.Entry<String, ModelNode> entry : currentValueTypes.entrySet()) {
                ModelNode currentEntry = entry.getValue();
                ModelNode legacyEntry = legacyValueTypes.get(entry.getKey());
                compareAttributeOrOperationParameter(context, "value-type key '" + entry.getKey() + "' for " + id, currentEntry, legacyEntry);
            }
        }
    }

    private void compareNillable(CompareContext context, String id, ModelNode current, ModelNode legacy) {
        boolean currentNillable = current.get(NILLABLE).asBoolean(false);
        boolean legacyNillable = legacy.get(NILLABLE).asBoolean(false);
        if (currentNillable != legacyNillable) {
            context.println("Different 'nillable' for " + id + ". Current: " + currentNillable + "; legacy: " + legacyNillable);
        }
    }

    private void compareExpressionsAllowed(CompareContext context, String id, ModelNode current, ModelNode legacy) {
        boolean currentNillable = current.get(EXPRESSIONS_ALLOWED).asBoolean(false);
        boolean legacyNillable = legacy.get(EXPRESSIONS_ALLOWED).asBoolean(false);
        if (currentNillable != legacyNillable) {
            context.println("Different 'expressions-allowed' for " + id + ". Current: " + currentNillable + "; legacy: " + legacyNillable);
        }
    }

    private void compareAccessType(CompareContext context, String id, ModelNode current, ModelNode legacy) {
        if (!current.get(ACCESS_TYPE).equals(legacy.get(ACCESS_TYPE))) {
            context.println("Different 'access-type' for " + id + ". Current: " + current.get(ACCESS_TYPE) + "; legacy: " + legacy.get(ACCESS_TYPE));
        }
    }

    private void compareStorage(CompareContext context, String id, ModelNode current, ModelNode legacy) {
        if (!current.get(STORAGE).equals(legacy.get(STORAGE))) {
            context.println("Different 'storage' for " + id + ". Current: " + current.get(STORAGE) + "; legacy: " + legacy.get(STORAGE));
        }
    }

    private boolean isType(ModelNode node) {
        if (!node.isDefined()) {
            return false;
        }
        try {
            node.asType();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, ModelNode> createMapIndexedByKey(ModelNode node){
        Map<String, ModelNode> map = new HashMap<String, ModelNode>();
        if (!node.isDefined()) {
            return map;
        }
        for (Property prop : node.asPropertyList()) {
            map.put(prop.getName(), prop.getValue());
        }
        return map;
    }

    private void compareChildren(CompareContext context) {
        Set<String> legacyChildTypes = context.getLegacyDefinition().getChildTypes();
        Set<String> currentChildTypes = context.getCurrentDefinition().getChildTypes();

        compareSetsAndRemoveMissing(context, "child types", currentChildTypes, legacyChildTypes);

        for (String type : legacyChildTypes) {
            Map<String, ModelNode> legacyChildren = context.getLegacyDefinition().getChildren(type);
            Map<String, ModelNode> currentChildren = context.getCurrentDefinition().getChildren(type);

            compareKeySetsAndRemoveMissing(context, "child names for type=" + type, currentChildren, legacyChildren);

            for (Map.Entry<String, ModelNode> legacyChildEntry : legacyChildren.entrySet()) {
                String name = legacyChildEntry.getKey();
                ModelNode legacyChildDescription = legacyChildEntry.getValue();
                ModelNode currentChildDescription = currentChildren.get(name);

                CompareContext childContext;
                try {
                    childContext = new CompareContext(
                            context.getRootAddress(),
                            context.getPathAddress().append(PathElement.pathElement(type, name)),
                            context.isCore(),
                            new ResourceDefinition(currentChildDescription, currentModelVersions),
                            new ResourceDefinition(legacyChildDescription, legacyModelVersions));
                } catch (RuntimeException e) {
                    System.out.println(context.getPathAddress() + " + " + type + "=" + name);
                    throw e;
                }
                compareModel(childContext);
            }
        }
    }

    private void compareKeySetsAndRemoveMissing(CompareContext context, String type, Map<String, ModelNode> currentMap, Map<String, ModelNode> legacyMap) {
        compareSetsAndRemoveMissing(context, type, currentMap.keySet(), legacyMap.keySet());
    }

    private void compareSetsAndRemoveMissing(CompareContext context, String type, Set<String> currentSet, Set<String> legacySet) {
        Set<String> extraInLegacy = getMissingNames(context, legacySet, currentSet);
        Set<String> extraInCurrent = getMissingNames(context, currentSet, legacySet);

        if (extraInLegacy.size() > 0 || extraInCurrent.size() > 0) {
            context.println("Missing " + type +
            		" in current: " + extraInLegacy + "; missing in legacy " + extraInCurrent);
            if (extraInCurrent.size() > 0) {
                currentSet.removeAll(extraInCurrent);
            }
            if (extraInLegacy.size() > 0) {
                legacySet.removeAll(extraInLegacy);
            }
        }
    }

    private Set<String> getMissingNames(CompareContext context, Set<String> possiblyMissing, Set<String> names){
        Set<String> missing = new HashSet<String>(possiblyMissing);
        for (String name : names) {
            missing.remove(name);
        }

        //7.1.2 did not have MANAGEMENT_MICRO_VERSION don't bother reporting that
        if (context.isVersionLevel() && missing.contains(MANAGEMENT_MICRO_VERSION) && names.contains(MANAGEMENT_MAJOR_VERSION) && names.contains(MANAGEMENT_MINOR_VERSION)) {
            missing.remove(MANAGEMENT_MICRO_VERSION);
        }

        return missing;
    }

    private class CompareContext {
        final PathAddress rootAddress;
        final PathAddress pathAddress;
        final boolean core;
        final ResourceDefinition legacyDefinition;
        final ResourceDefinition currentDefinition;
        boolean outputPath;

        CompareContext(PathAddress rootAddress, PathAddress pathAddress, boolean core, ResourceDefinition currentDefinition, ResourceDefinition legacyDefinition) {
            this.rootAddress = rootAddress;
            this.pathAddress = pathAddress;
            this.core = core;
            this.currentDefinition = currentDefinition;
            this.legacyDefinition = legacyDefinition;
        }

        PathAddress getRootAddress() {
            return rootAddress;
        }


        PathAddress getPathAddress() {
            return pathAddress;
        }

        boolean isVersionLevel() {
            return rootAddress.equals(pathAddress);
        }

        boolean isCore() {
            return core;
        }

        ResourceDefinition getLegacyDefinition() {
            return legacyDefinition;
        }

        ResourceDefinition getCurrentDefinition() {
            return currentDefinition;
        }

        boolean continuteWithCheck() {
            if (!isVersionLevel()) {
                return true;
            }
            ModelVersion currentVersion = getModelVersion(currentDefinition);
            ModelVersion legacyVersion = getModelVersion(legacyDefinition);
            System.out.println("====== Resource root address: " + formatAddressOneLine(pathAddress) + " - Current version: " + currentVersion + "; legacy version: " + legacyVersion + " =======");

            if (!legacyVersion.equals(currentVersion) && compareDifferentVersions) {
                return true;
            } else if (legacyVersion.equals(currentVersion)){
                return true;
            } else {
                System.out.println("Skipping check of resource and children");
                return false;
            }
        }

        private ModelVersion getModelVersion(ResourceDefinition definition) {
            if (core) {
                return definition.getCoreModelVersion();
            } else {
                return definition.getSubsystemVersion(pathAddress);
            }
        }

        private String formatAddressOneLine(PathAddress addr) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (PathElement element : addr) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(element);
            }
            sb.append("]");
            return sb.toString();
        }

        void println(String msg) {
            if (!outputPath) {
                outputPath = true;
                PathAddress relative = pathAddress.subAddress(rootAddress.size());
                System.out.println("--- Problems for relative address to root " + formatAddressOneLine(relative) + ":");
            }
            System.out.println(msg);
        }
    }


    private static class ResourceDefinition {
        final ModelNode description;
        final ModelNode versions;

        ResourceDefinition(ModelNode description, ModelNode versions) {
            this.description = description;
            this.versions = versions;
        }


        Map<String, ModelNode> getAttributes() {
            return getSortedEntryMap(description, ATTRIBUTES);
        }

        Map<String, ModelNode> getOperations() {
            return getSortedEntryMap(description, OPERATIONS);
        }

        Set<String> getChildTypes() {
            return getSortedEntryMap(description, CHILDREN).keySet();
        }

        Map<String, ModelNode> getChildren(String type){
            return getSortedEntryMap(description.get(CHILDREN, type), MODEL_DESCRIPTION);
        }

        Map<String, ModelNode> getOperationParameters(String opName) {
            return getSortedEntryMap(description.get(OPERATIONS, opName), REQUEST_PROPERTIES);
        }

        private Map<String, ModelNode> getSortedEntryMap(ModelNode parent, String name){
            if (!parent.hasDefined(name)) {
                return Collections.emptyMap();
            }
            Map<String, ModelNode> sorted = new TreeMap<String, ModelNode>();
            for (Property prop : parent.get(name).asPropertyList()) {
                sorted.put(prop.getName(), prop.getValue());
            }
            return sorted;
        }

        private ModelVersion getCoreModelVersion() {
            return Tools.createModelVersion(versions.get(Tools.CORE, Tools.STANDALONE));
        }

        private ModelVersion getSubsystemVersion(PathAddress address) {
            for (PathElement element : address) {
                if (element.getKey().equals(SUBSYSTEM)) {
                    return Tools.createModelVersion(versions.get(SUBSYSTEM, element.getValue()));
                }
            }
            throw new IllegalArgumentException("Could not find subsystem version for " + address);
        }
    }

}
