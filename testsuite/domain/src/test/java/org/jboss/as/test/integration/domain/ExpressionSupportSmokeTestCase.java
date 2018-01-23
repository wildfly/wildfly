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

package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALTERNATIVES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STORAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.createOperation;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.dmr.ValueExpression;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Smoke test of expression support.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ExpressionSupportSmokeTestCase extends BuildConfigurationTestBase {
    private static final Logger LOGGER = Logger.getLogger(ExpressionSupportSmokeTestCase.class);

    private static final Set<ModelType> COMPLEX_TYPES = Collections.unmodifiableSet(EnumSet.of(ModelType.LIST, ModelType.OBJECT, ModelType.PROPERTY));

    private DomainLifecycleUtil domainMasterLifecycleUtil;

    private int conflicts;
    private int noSimple;
    private int noSimpleCollection;
    private int noComplexList;
    private int noObject;
    private int noComplexProperty;
    private int supportedUndefined;
    private int simple;
    private int simpleCollection;
    private int complexList;
    private int object;
    private int complexProperty;

    private final boolean immediateValidation = Boolean.getBoolean("immediate.expression.validation");
    private final boolean logHandling = Boolean.getBoolean("expression.logging");

    /**
     * Launch a master HC in --admin-only. Iterate through all resources, converting any writable attribute that
     * support expressions and has a value or a default value to an expression (if it isn't already one), using the
     * value/default value as the expression default (so setting a system property isn't required). Then reload out of
     * --admin-only and confirm that the host's servers start correctly. Finally, read the resources from the host
     * and confirm that the expected values are there.
     *
     * @throws Exception  if there is one
     */
    @Test
    public void test() throws Exception {

        // Add some extra resources to get some more coverage
        addTestResources();

        Map<PathAddress, Map<String, ModelNode>> expectedValues = new HashMap<PathAddress, Map<String, ModelNode>>();
        setExpressions(PathAddress.EMPTY_ADDRESS, "master", expectedValues);

        LOGGER.trace("Update statistics:");
        LOGGER.trace("==================");
        LOGGER.trace("conflicts: " + conflicts);
        LOGGER.trace("no expression simple: " + noSimple);
        LOGGER.trace("no expression simple collection: " + noSimpleCollection);
        LOGGER.trace("no expression complex list: " + noComplexList);
        LOGGER.trace("no expression object: " + noObject);
        LOGGER.trace("no expression complex property: " + noComplexProperty);
        LOGGER.trace("supported but undefined: " + supportedUndefined);
        LOGGER.trace("simple: " + simple);
        LOGGER.trace("simple collection: " + simpleCollection);
        LOGGER.trace("complex list: " + complexList);
        LOGGER.trace("object: " + object);
        LOGGER.trace("complex property: " + complexProperty);

        // restart back to normal mode
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).add(HOST, "master");
        op.get(OP).set("reload");
        op.get("admin-only").set(false);
        domainMasterLifecycleUtil.executeAwaitConnectionClosed(op);

        // Try to reconnect to the hc
        domainMasterLifecycleUtil.connect();

        // check that the servers are up
        domainMasterLifecycleUtil.awaitServers(System.currentTimeMillis());

        validateExpectedValues(PathAddress.EMPTY_ADDRESS, expectedValues, "master");
    }

    @Before
    public void setUp() throws IOException {
        final WildFlyManagedConfiguration config = createConfiguration("domain.xml", "host.xml", getClass().getSimpleName());
        config.setAdminOnly(true);
        config.setReadOnlyDomain(true);
        config.setReadOnlyHost(true);

        // Trigger the servers to fail on boot if there are runtime errors
        String hostProps = config.getHostCommandLineProperties();
        hostProps = hostProps == null ? "" : hostProps;
        config.setHostCommandLineProperties(hostProps + "\n-Djboss.unsupported.fail-boot-on-runtime-failure=true");

        domainMasterLifecycleUtil = new DomainLifecycleUtil(config);
//        domainMasterLifecycleUtil.getConfiguration().addHostCommandLineProperty("-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y");
        domainMasterLifecycleUtil.start(); // Start

        conflicts = noSimple = noSimpleCollection = noComplexList = noComplexProperty = noObject = noComplexProperty =
                supportedUndefined = simple = simpleCollection = object = complexProperty = complexList = 0;
    }

    @After
    public void tearDown() {
        if (domainMasterLifecycleUtil != null) {
            domainMasterLifecycleUtil.stop();
        }
    }

    private void addTestResources() throws IOException, MgmtOperationException {
        PathAddress spAddr = PathAddress.pathAddress(PathElement.pathElement(SYSTEM_PROPERTY, "domain-test"));
        ModelNode op = createOperation(ADD, spAddr);
        op.get(VALUE).set("test");
        op.get(BOOT_TIME).set(true);
        executeForResult(op, domainMasterLifecycleUtil.getDomainClient());

        PathAddress hostSpAddr = PathAddress.pathAddress(PathElement.pathElement(HOST, "master"), PathElement.pathElement(SYSTEM_PROPERTY, "host-test"));
        op.get(OP_ADDR).set(hostSpAddr.toModelNode());
        executeForResult(op, domainMasterLifecycleUtil.getDomainClient());

        PathAddress pathAddr = PathAddress.pathAddress(PathElement.pathElement(PATH, "domain-path-test"));
        op = createOperation(ADD, pathAddr);
        op.get(RELATIVE_TO).set("jboss.home.dir");
        op.get(PATH).set("test");
        executeForResult(op, domainMasterLifecycleUtil.getDomainClient());

        PathAddress hostPathAddr = PathAddress.pathAddress(PathElement.pathElement(HOST, "master"), PathElement.pathElement(PATH, "host-path-test"));
        op.get(OP_ADDR).set(hostPathAddr.toModelNode());
        executeForResult(op, domainMasterLifecycleUtil.getDomainClient());
    }

    private void setExpressions(PathAddress address, String hostName, Map<PathAddress, Map<String, ModelNode>> expectedValues) throws IOException, MgmtOperationException {

        ModelNode description = readResourceDescription(address);
        ModelNode resource = readResource(address, true, false);
        ModelNode resourceNoDefaults = readResource(address, false, false);

        Map<String, ModelNode> expressionAttrs = new HashMap<String, ModelNode>();
        Map<String, ModelNode> otherAttrs = new HashMap<String, ModelNode>();
        Map<String, ModelNode> expectedAttrs = new HashMap<String, ModelNode>();
        organizeAttributes(address, description, resource, resourceNoDefaults, expressionAttrs, otherAttrs, expectedAttrs);


        for (Map.Entry<String, ModelNode> entry : expressionAttrs.entrySet()) {
            writeAttribute(address, entry.getKey(), entry.getValue());
        }
        // Set the other attrs as well just to exercise the write-attribute handlers
        for (Map.Entry<String, ModelNode> entry : otherAttrs.entrySet()) {
            writeAttribute(address, entry.getKey(), entry.getValue());
        }

        if (expectedAttrs.size() > 0 && immediateValidation) {
            // Validate that our write-attribute calls resulted in the expected values in the model
            ModelNode modifiedResource = readResource(address, true, true);
            for (Map.Entry<String, ModelNode> entry : expectedAttrs.entrySet()) {
                ModelNode expectedValue = entry.getValue();
                ModelNode modVal = modifiedResource.get(entry.getKey());
                validateAttributeValue(address, entry.getKey(), expectedValue, modVal);
            }
        }

        // Store the modified values for confirmation after HC reload
        expectedValues.put(address, expectedAttrs);

        // Recurse into children, being careful about what processes we are touching
        boolean isHost = address.size() == 1 && HOST.equals(address.getLastElement().getKey());
        for (Property descProp : description.get(CHILDREN).asPropertyList()) {
            String childType = descProp.getName();
            if (isHost && SERVER.equals(childType)) {
                continue;
            }
            boolean hostChild = address.size() == 0 && HOST.equals(childType);
            List<String> children = readChildrenNames(address, childType);
            for (String child : children) {
                if (!hostChild || hostName.equals(child)) {
                    setExpressions(address.append(PathElement.pathElement(childType, child)), hostName, expectedValues);
                }
            }
        }
    }

    private void organizeAttributes(PathAddress address, ModelNode description, ModelNode resource, ModelNode resourceNoDefaults,
                                    Map<String, ModelNode> expressionAttrs, Map<String, ModelNode> otherAttrs,
                                    Map<String, ModelNode> expectedAttrs) {
        ModelNode attributeDescriptions = description.get(ATTRIBUTES);
        for (Property descProp : attributeDescriptions.asPropertyList()) {
            String attrName = descProp.getName();
            ModelNode attrDesc = descProp.getValue();
            if (isAttributeExcluded(address, attrName, attrDesc, resourceNoDefaults)) {
                continue;
            }
            ModelNode noDefaultValue = resourceNoDefaults.get(attrName);
            if (!noDefaultValue.isDefined()) {
                // We need to see if it's legal to set this attribute, or whether it's undefined
                // because an alternative attribute is defined or a required attribute is not defined.
                Set<String> base = new HashSet<String>();
                base.add(attrName);
                if (attrDesc.hasDefined(REQUIRES)) {
                    for (ModelNode node : attrDesc.get(REQUIRES).asList()) {
                        base.add(node.asString());
                    }
                }
                boolean conflict = false;
                for (String baseAttr : base) {
                    if (!resource.hasDefined(baseAttr)) {
                        conflict = true;
                        break;
                    }

                    ModelNode baseAttrAlts = attributeDescriptions.get(baseAttr, ALTERNATIVES);
                    if (baseAttrAlts.isDefined()) {
                        for (ModelNode alt : baseAttrAlts.asList()) {
                            String altName = alt.asString();
                            if (resourceNoDefaults.hasDefined(alt.asString())
                                    || expressionAttrs.containsKey(altName)
                                    || otherAttrs.containsKey(altName)) {
                                conflict = true;
                                break;
                            }
                        }
                    }
                }
                if (conflict) {
                    conflicts++;
                    logHandling("Skipping conflicted attribute " + attrName + " at " + address.toModelNode().asString());
                    continue;
                }
            }
            ModelNode attrValue = resource.get(attrName);
            ModelType attrType = attrValue.getType();
            if (attrDesc.get(EXPRESSIONS_ALLOWED).asBoolean(false)) {

                // If it's defined and not an expression, use the current value to create an expression
                if (attrType != ModelType.UNDEFINED && attrType != ModelType.EXPRESSION) {
                    // Deal with complex types specially
                    if (COMPLEX_TYPES.contains(attrType)) {
                        ModelNode valueType = attrDesc.get(VALUE_TYPE);
                        if (valueType.getType() == ModelType.TYPE) {
                            // Simple collection whose elements support expressions
                            handleSimpleCollection(address, attrName, attrValue, valueType.asType(), expressionAttrs,
                                    otherAttrs, expectedAttrs);
                        } else if (valueType.isDefined()) {
                            handleComplexCollection(address, attrName, attrValue, attrType, valueType, expressionAttrs, otherAttrs,
                                    expectedAttrs);
                        } else {
                            noSimple++;
                            logNoExpressions(address, attrName);
                            otherAttrs.put(attrName, attrValue);
                            expectedAttrs.put(attrName, attrValue);
                        }
                    } else {
                        if (attrType == ModelType.STRING) {
                            checkForUnconvertedExpression(address, attrName, attrValue);
                        }
                        String expression = "${exp.test:" + attrValue.asString() + "}";
                        expressionAttrs.put(attrName, new ModelNode(expression));
                        expectedAttrs.put(attrName, new ModelNode().set(new ValueExpression(expression)));
                        simple++;
                        logHandling("Added expression to simple attribute " + attrName + " at " + address.toModelNode().asString());
                    }
                } else {
                    if (attrType != ModelType.EXPRESSION) {
                        supportedUndefined++;
                        logHandling("Expression supported but value undefined on simple attribute " + attrName + " at " + address.toModelNode().asString());
                    } else {
                        simple++;
                        logHandling("Already found an expression on simple attribute " + attrName + " at " + address.toModelNode().asString());
                    }
                    otherAttrs.put(attrName, attrValue);
                    expectedAttrs.put(attrName, attrValue);
                }
            } else if (COMPLEX_TYPES.contains(attrType)
                    && attrDesc.get(VALUE_TYPE).getType() != ModelType.TYPE
                    && attrDesc.get(VALUE_TYPE).isDefined()) {
                handleComplexCollection(address, attrName, attrValue, attrType, attrDesc.get(VALUE_TYPE), expressionAttrs,
                        otherAttrs, expectedAttrs);
            } else /*if (!attrDesc.hasDefined(DEPRECATED))*/ {
                noSimple++;
                logNoExpressions(address, attrName);
                otherAttrs.put(attrName, attrValue);
                expectedAttrs.put(attrName, attrValue);
            }
        }
    }

    private boolean isAttributeExcluded(PathAddress address, String attrName, ModelNode attrDesc, ModelNode resourceNoDefaults) {
        if (!attrDesc.get(ACCESS_TYPE).isDefined()
                || !attrDesc.get(ACCESS_TYPE).asString().equalsIgnoreCase("read-write")) {
            return true;
        }
        if (attrDesc.get(STORAGE).isDefined()
                && !attrDesc.get(STORAGE).asString().equalsIgnoreCase("configuration")) {
            return true;
        }
        if (attrDesc.get(ModelDescriptionConstants.DEPRECATED).isDefined()){
            return true;
        }


        // Special cases
        if ("default-web-module".equals(attrName)) {
            if (address.size() > 1) {
                PathElement subPe = address.getElement(address.size() - 2);
                if ("subsystem".equals(subPe.getKey()) && "web".equals(subPe.getValue())
                        && "virtual-server".equals(address.getLastElement().getKey())) {
                    // This is not allowed if "enable-welcome-root" is "true", which is overly complex to validate
                    // so skip it
                    return true;
                }
            }
        } else if ("policy-modules".equals(attrName) || "login-modules".equals(attrName)) {
            if (address.size() > 2) {
                PathElement subPe = address.getElement(address.size() - 3);
                if ("subsystem".equals(subPe.getKey()) && "security".equals(subPe.getValue())
                        && "security-domain".equals(address.getElement(address.size() - 2).getKey())) {
                    // This is a kind of alias that shows child resources as a list. Validating it
                    // after reload breaks because the real child resources get changed. It's deprecated, so
                    // we could exclude all deprecated attributes, but for now I'd rather be specific
                    return true;
                }
            }
        } else if ("virtual-nodes".equals(attrName)) {
            if (address.size() > 3) {
                PathElement subPe = address.getElement(address.size() - 3);
                PathElement containerPe = address.getElement(address.size() - 2);
                PathElement distPe = address.getElement(address.size()-1);
                if ("subsystem".equals(subPe.getKey()) && "infinispan".equals(subPe.getValue())
                        && "cache-container".equals(containerPe.getKey())
                        && "distributed-cache".equals(distPe.getKey())) {
                    // This is a distributed cache attribute in Infinispan which has been deprecated
                    return true;
                }
            }
        } else if (address.size() > 0 && "transactions".equals(address.getLastElement().getValue())
                &&  "subsystem".equals(address.getLastElement().getKey())) {
            if (attrName.contains("jdbc")) {
                // Ignore jdbc store attributes unless jdbc store is enabled
                return !resourceNoDefaults.hasDefined("use-jdbc-store") || !resourceNoDefaults.get("use-jdbc-store").asBoolean();
            } else if (attrName.contains("journal")) {
                // Ignore journal store attributes unless journal store is enabled
                return !resourceNoDefaults.hasDefined("use-journal-store") || !resourceNoDefaults.get("use-journal-store").asBoolean();
            }
        }

        return false;
    }

    private void logNoExpressions(PathAddress address, String attrName) {
        if (logHandling) {
            logHandling("No expression support for attribute " + attrName + " at " + address.toModelNode().asString());
        }
    }

    private void logHandling(String msg) {
        if (logHandling) {
            LOGGER.trace(msg);
        }
    }

    private void handleSimpleCollection(PathAddress address, String attrName, ModelNode attrValue, ModelType valueType,
                                        Map<String, ModelNode> expressionAttrs, Map<String, ModelNode> otherAttrs,
                                        Map<String, ModelNode> expectedAttrs) {
        if (COMPLEX_TYPES.contains(valueType)) {
            // Too complicated
            noSimpleCollection++;
            logNoExpressions(address, attrName);
            otherAttrs.put(attrName, attrValue);
        } else {
            boolean hasExpression = false;
            ModelNode updated = new ModelNode();
            ModelNode expected = new ModelNode();
            for (ModelNode item : attrValue.asList()) {
                ModelType itemType = item.getType();
                if (itemType == ModelType.PROPERTY) {
                    Property prop = item.asProperty();
                    ModelNode propVal = prop.getValue();
                    ModelType propValType = propVal.getType();
                    if (propVal.isDefined() && propValType != ModelType.EXPRESSION) {
                        // Convert property value to expression
                        if (propValType == ModelType.STRING) {
                            checkForUnconvertedExpression(address, attrName, propVal);
                        }
                        String expression = "${exp.test:" + propVal.asString() + "}";
                        updated.get(prop.getName()).set(expression);
                        expected.get(prop.getName()).set(new ModelNode().set(new ValueExpression(expression)));
                        hasExpression = true;

                    } else {
                        updated.get(prop.getName()).set(propVal);
                        expected.get(prop.getName()).set(propVal);
                    }
                } else if (item.isDefined() && itemType != ModelType.EXPRESSION) {
                    // Convert item to expression
                    if (itemType == ModelType.STRING) {
                        checkForUnconvertedExpression(address, attrName, item);
                    }
                    String expression = "${exp.test:" + item.asString() + "}";
                    updated.add(expression);
                    expected.add(new ModelNode().set(new ValueExpression(expression)));
                    hasExpression = true;
                } else {
                    updated.add(item);
                    expected.add(item);
                }
            }

            if (hasExpression) {
                simpleCollection++;
                logHandling("Added expression to SIMPLE " + attrValue.getType() + " attribute " + attrName + " at " + address.toModelNode().asString());
                expressionAttrs.put(attrName, updated);
                expectedAttrs.put(attrName, expected);
            } else {
                // We didn't change anything
                noSimpleCollection++;
                logNoExpressions(address, attrName);
                otherAttrs.put(attrName, attrValue);
                expectedAttrs.put(attrName, attrValue);
            }
        }
    }

    private void handleComplexCollection(PathAddress address, String attrName, ModelNode attrValue,
                                         ModelType attrType, ModelNode valueTypeDesc,
                                         Map<String, ModelNode> expressionAttrs, Map<String, ModelNode> otherAttrs,
                                         Map<String, ModelNode> expectedAttrs) {
        switch (attrType) {
            case LIST:
                handleComplexList(address, attrName, attrValue, valueTypeDesc, expressionAttrs, otherAttrs,
                        expectedAttrs);
                break;
            case OBJECT:
                handleComplexObject(address, attrName, attrValue, valueTypeDesc, expressionAttrs, otherAttrs,
                        expectedAttrs);
                break;
            case PROPERTY:
                handleComplexProperty(address, attrName, attrValue, valueTypeDesc, expressionAttrs, otherAttrs,
                        expectedAttrs);
                break;
            default:
                throw new IllegalArgumentException(attrType.toString());
        }
    }

    private void handleComplexList(PathAddress address, String attrName, ModelNode attrValue, ModelNode valueTypeDesc,
                                         Map<String, ModelNode> expressionAttrs, Map<String, ModelNode> otherAttrs,
                                         Map<String, ModelNode> expectedAttrs) {
        ModelNode updatedList = new ModelNode().setEmptyList();
        ModelNode expectedList = new ModelNode().setEmptyList();
        boolean changed = false;

        for (ModelNode item : attrValue.asList()) {
            ModelNode updated = new ModelNode();
            ModelNode toExpect = new ModelNode();
            handleComplexItem(address, attrName, item, valueTypeDesc, updated, toExpect);
            changed |= !updated.equals(item);
            updatedList.add(updated);
            expectedList.add(toExpect);
        }

        if (changed) {
            complexList++;
            logHandling("Added expression to COMPLEX LIST attribute " + attrName + " at " + address.toModelNode().asString());
            expressionAttrs.put(attrName, updatedList);
            expectedAttrs.put(attrName, expectedList);
        } else {
            noComplexList++;
            logNoExpressions(address, attrName);
            otherAttrs.put(attrName, attrValue);
        }
    }

    private void handleComplexObject(PathAddress address, String attrName, ModelNode attrValue, ModelNode valueTypeDesc,
                                         Map<String, ModelNode> expressionAttrs, Map<String, ModelNode> otherAttrs,
                                         Map<String, ModelNode> expectedAttrs) {

        ModelNode updated = new ModelNode();
        ModelNode toExpect = new ModelNode();
        handleComplexItem(address, attrName, attrValue, valueTypeDesc, updated, toExpect);
        if (!updated.equals(attrValue)) {
            object++;
            logHandling("Added expression to OBJECT attribute " + attrName + " at " + address.toModelNode().asString());
            expressionAttrs.put(attrName, updated);
            expectedAttrs.put(attrName, toExpect);
        } else {
            noObject++;
            logNoExpressions(address, attrName);
            otherAttrs.put(attrName, attrValue);
        }
    }

    private void handleComplexProperty(PathAddress address, String attrName, ModelNode attrValue, ModelNode valueTypeDesc,
                                         Map<String, ModelNode> expressionAttrs, Map<String, ModelNode> otherAttrs,
                                         Map<String, ModelNode> expectedAttrs) {
        Property prop = attrValue.asProperty();
        ModelNode propVal = prop.getValue();
        ModelNode updatedPropVal = new ModelNode();
        ModelNode propValToExpect = new ModelNode();
        handleComplexItem(address, attrName, propVal, valueTypeDesc, updatedPropVal, propValToExpect);
        if (!updatedPropVal.equals(propVal)) {
            complexProperty++;
            ModelNode updatedProp = new ModelNode().set(prop.getName(), updatedPropVal);
            logHandling("Added expression to COMPLEX PROPERTY attribute " + attrName + " at " + address.toModelNode().asString());
            expressionAttrs.put(attrName, updatedProp);
            expectedAttrs.put(attrName, new ModelNode().set(prop.getName(), propValToExpect));
        } else {
            noComplexProperty++;
            logNoExpressions(address, attrName);
            otherAttrs.put(attrName, attrValue);
        }
    }

    private void handleComplexItem(PathAddress address, String attrName, ModelNode item, ModelNode valueTypeDesc,
                                   ModelNode updatedItem, ModelNode itemToExpect) {
        // Hack to deal with time unit processing
        Set<String> keys =  valueTypeDesc.keys();
        boolean timeAttr = keys.size() == 2 && keys.contains("time") && keys.contains("unit");
        boolean changed = false;
        for (Property fieldProp : valueTypeDesc.asPropertyList()) {
            String fieldName = fieldProp.getName();
            if (!item.has(fieldName)) {
                continue;
            }
            boolean timeunit = timeAttr && "unit".equals(fieldName);
            ModelNode fieldDesc = fieldProp.getValue();
            ModelNode fieldValue = item.get(fieldName);
            ModelType valueType = fieldValue.getType();
            if (valueType == ModelType.UNDEFINED
                    || valueType == ModelType.EXPRESSION
                    || COMPLEX_TYPES.contains(valueType) // too complex
                    || !fieldDesc.get(EXPRESSIONS_ALLOWED).asBoolean(false)) {
                updatedItem.get(fieldName).set(fieldValue);
                itemToExpect.get(fieldName).set(fieldValue);
            } else {
                if (valueType == ModelType.STRING) {
                    checkForUnconvertedExpression(address, attrName, item);
                }
                String valueString = timeunit ? fieldValue.asString().toLowerCase() : fieldValue.asString();
                String expression = "${exp.test:" + valueString + "}";
                updatedItem.get(fieldName).set(expression);
                itemToExpect.get(fieldName).set(new ModelNode().set(new ValueExpression(expression)));
                changed = true;
            }
        }

        if (!changed) {
            // Use unchanged 'item'
            updatedItem.set(item);
            itemToExpect.set(item);
        }
    }

    private ModelNode readResourceDescription(PathAddress address) throws IOException, MgmtOperationException {

        ModelNode op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, address);
        return executeForResult(op, domainMasterLifecycleUtil.getDomainClient());
    }

    private ModelNode readResource(PathAddress address, boolean defaults, boolean failIfMissing) throws IOException, MgmtOperationException {

        try {
            ModelNode op = createOperation(READ_RESOURCE_OPERATION, address);
            op.get(INCLUDE_DEFAULTS).set(defaults);
            return executeForResult(op, domainMasterLifecycleUtil.getDomainClient());
        } catch (MgmtOperationException e) {
            if (failIfMissing) {
                throw e;
            }
            return new ModelNode();
        }
    }

    private void checkForUnconvertedExpression(PathAddress address, String attrName, ModelNode attrValue) {
        String text = attrValue.asString();
        int start = text.indexOf("${");
        if (start > -1) {
            if (text.indexOf("}") > start) {
                Assert.fail(address + " attribute " + attrName + " is storing an unconverted expression: " + text);
            }
        }
    }

    private void writeAttribute(PathAddress address, String attrName, ModelNode value) throws IOException, MgmtOperationException {

        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(attrName);
        op.get(VALUE).set(value);
        executeForResult(op, domainMasterLifecycleUtil.getDomainClient());
    }

    private List<String> readChildrenNames(PathAddress address, String childType) throws IOException, MgmtOperationException {

        ModelNode op = createOperation(READ_CHILDREN_NAMES_OPERATION, address);
        op.get(CHILD_TYPE).set(childType);
        ModelNode opResult = executeForResult(op, domainMasterLifecycleUtil.getDomainClient());
        List<String> result = new ArrayList<String>();
        for (ModelNode child : opResult.asList()) {
            result.add(child.asString());
        }
        return result;
    }

    private void validateExpectedValues(PathAddress address, Map<PathAddress, Map<String, ModelNode>> expectedValues, String hostName) throws IOException, MgmtOperationException {

        Map<String, ModelNode> expectedModel = expectedValues.get(address);
        if (expectedModel != null && isValidatable(address)) {
            ModelNode resource = readResource(address, true, true);
            for (Map.Entry<String, ModelNode> entry : expectedModel.entrySet()) {
                String attrName = entry.getKey();
                ModelNode expectedValue = entry.getValue();
                ModelNode modVal = resource.get(entry.getKey());
                validateAttributeValue(address, attrName, expectedValue, modVal);
            }
        }

        ModelNode description = readResourceDescription(address);

        // Recurse into children, being careful about what processes we are touching
        boolean isHost = address.size() == 1 && HOST.equals(address.getLastElement().getKey());
        for (Property descProp : description.get(CHILDREN).asPropertyList()) {
            String childType = descProp.getName();
            if (isHost && SERVER.equals(childType)) {
                continue;
            }
            boolean hostChild = address.size() == 0 && HOST.equals(childType);
            List<String> children = readChildrenNames(address, childType);
            for (String child : children) {
                if (!hostChild || hostName.equals(child)) {
                    validateExpectedValues(address.append(PathElement.pathElement(childType, child)), expectedValues, hostName);
                }
            }
        }
    }

    private void validateAttributeValue(PathAddress address, String attrName, ModelNode expectedValue, ModelNode modelValue) {
        switch (expectedValue.getType()) {
            case EXPRESSION: {
                Assert.assertEquals(address + " attribute " + attrName + " value " + modelValue + " is an unconverted expression",
                        expectedValue, modelValue);
                break;
            }
            case INT:
            case LONG:
                Assert.assertTrue(address + " attribute " + attrName + " is a valid type", modelValue.getType() == ModelType.INT || modelValue.getType() == ModelType.LONG);
                Assert.assertEquals(address + " -- " + attrName, expectedValue.asLong(), modelValue.asLong());
                break;
            default: {
                Assert.assertEquals(address + " -- " + attrName, expectedValue, modelValue);
            }
        }
    }

    private boolean isValidatable(PathAddress address) {
        boolean result = true;
        if (address.size() > 1 && address.getLastElement().getKey().equals("bootstrap-context") && address.getLastElement().getValue().equals("default")
            && address.getElement(address.size() - 2).getKey().equals("subsystem") && address.getElement(address.size() - 2).getValue().equals("jca")) {
            // JCA subsystem doesn't persist this resource
            result = false;
        }
        return result;
    }

}
