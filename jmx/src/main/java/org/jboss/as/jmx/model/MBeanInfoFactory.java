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
package org.jboss.as.jmx.model;

import static javax.management.JMX.DEFAULT_VALUE_FIELD;
import static javax.management.JMX.LEGAL_VALUES_FIELD;
import static javax.management.JMX.MAX_VALUE_FIELD;
import static javax.management.JMX.MIN_VALUE_FIELD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfo;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.AttributeAccess.AccessType;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.jmx.model.ChildAddOperationFinder.ChildAddOperationEntry;
import org.jboss.as.server.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MBeanInfoFactory {

    private final String DESC_MBEAN_EXPR = "mbean.expression.support";
    private final String DESC_MBEAN_EXPR_DESCR = "mbean.expression.support.description";
    private final String DESC_ALTERNATE_MBEAN = "alternate.mbean";
    private final String DESC_ALTERNATE_MBEAN_DESCR = "alternate.mbean.description";
    private final String DESC_EXPRESSIONS_ALLOWED = "expressions.allowed";
    private final String DESC_EXPRESSIONS_ALLOWED_DESC = "expressions.allowed.description";

    private static final OpenMBeanParameterInfo[] EMPTY_PARAMETERS = new OpenMBeanParameterInfo[0];
    private final ObjectName name;
    private final TypeConverters converters;
    private final ConfiguredDomains configuredDomains;
    private final boolean standalone;
    private final ImmutableManagementResourceRegistration resourceRegistration;
    private final ModelNode providedDescription;
    private final PathAddress pathAddress;
    private final boolean legacy;

    private MBeanInfoFactory(final ObjectName name, final TypeConverters converters, final ConfiguredDomains configuredDomains, final boolean standalone, final PathAddress address, final ImmutableManagementResourceRegistration resourceRegistration) {
        this.name = name;
        this.converters = converters;
        this.configuredDomains = configuredDomains;
        this.standalone = standalone;
        this.legacy = configuredDomains.isLegacyDomain(name);
        this.resourceRegistration = resourceRegistration;
        DescriptionProvider provider = resourceRegistration.getModelDescription(PathAddress.EMPTY_ADDRESS);
        providedDescription = provider != null ? provider.getModelDescription(null) : new ModelNode();
        this.pathAddress = address;
    }

    static MBeanInfo createMBeanInfo(final ObjectName name, final TypeConverters converters, final ConfiguredDomains configuredDomains, final boolean standalone, final PathAddress address, final ImmutableManagementResourceRegistration resourceRegistration) throws InstanceNotFoundException{
        return new MBeanInfoFactory(name, converters, configuredDomains, standalone, address, resourceRegistration).createMBeanInfo();
    }

    private MBeanInfo createMBeanInfo() {
        return new OpenMBeanInfoSupport(ModelControllerMBeanHelper.CLASS_NAME,
                getDescription(providedDescription),
                getAttributes(),
                getConstructors(),
                getOperations(),
                getNotifications(),
                createMBeanDescriptor());
    }


    private String getDescription(ModelNode node) {
        if (!node.hasDefined(DESCRIPTION)) {
            return "-";
        }
        String description = node.get(DESCRIPTION).asString();
        if (description.trim().length() == 0) {
            return "-";
        }
        return description;
    }

    private OpenMBeanAttributeInfo[] getAttributes() {
        List<OpenMBeanAttributeInfo> infos = new ArrayList<OpenMBeanAttributeInfo>();
        if (providedDescription.hasDefined(ATTRIBUTES)) {
            for (final String name : providedDescription.require(ATTRIBUTES).keys()) {
                infos.add(getAttribute(name));
            }
        }
        return infos.toArray(new OpenMBeanAttributeInfo[infos.size()]);
    }

    private OpenMBeanAttributeInfo getAttribute(String name) {
        final String escapedName = NameConverter.convertToCamelCase(name);
        ModelNode attribute = providedDescription.require(ATTRIBUTES).require(name);
        AttributeAccess access = resourceRegistration.getAttributeAccess(PathAddress.EMPTY_ADDRESS, name);
        final boolean writable;
        if (!standalone) {
            writable = false;
        } else {
            writable = access != null ? access.getAccessType() == AccessType.READ_WRITE : false;
        }

        return new OpenMBeanAttributeInfoSupport(
                escapedName,
                getDescription(attribute),
                converters.convertToMBeanType(attribute),
                true,
                writable,
                false,
                createAttributeDescriptor(attribute));
    }

    private OpenMBeanConstructorInfo[] getConstructors() {
        //This can be left empty
        return null;
    }

    private OpenMBeanOperationInfo[] getOperations() {
        final boolean root = pathAddress.size() == 0;

        //TODO include inherited/global operations?
        List<OpenMBeanOperationInfo> ops = new ArrayList<OpenMBeanOperationInfo>();
        for (Map.Entry<String, OperationEntry> entry : resourceRegistration.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, false).entrySet()) {
            final String opName = entry.getKey();
            if (opName.equals(ADD) || opName.equals(DESCRIBE)) {
                continue;
            }
            if (root) {
                if (opName.equals(READ_RESOURCE_OPERATION) || opName.equals(READ_ATTRIBUTE_OPERATION) ||
                        opName.equals(READ_RESOURCE_DESCRIPTION_OPERATION) || opName.equals(READ_CHILDREN_NAMES_OPERATION) ||
                        opName.equals(READ_CHILDREN_TYPES_OPERATION) || opName.equals(READ_CHILDREN_RESOURCES_OPERATION) ||
                        opName.equals(READ_OPERATION_NAMES_OPERATION) || opName.equals(READ_OPERATION_DESCRIPTION_OPERATION) ||
                        opName.equals(READ_RESOURCE_OPERATION) || opName.equals(READ_RESOURCE_OPERATION) ||
                        opName.equals(WRITE_ATTRIBUTE_OPERATION) || opName.equals(ValidateAddressOperationHandler.OPERATION_NAME) ||
                        opName.equals(CompositeOperationHandler.NAME) || opName.equals(DeploymentUploadStreamAttachmentHandler.OPERATION_NAME) ||
                        opName.equals(RootResourceHack.NAME)) {
                    //Ignore some of the global operations which probably don't make much sense here
                    continue;
                }
            }
            final OperationEntry opEntry = entry.getValue();
            if (standalone ? true : opEntry.getFlags().contains(OperationEntry.Flag.READ_ONLY)) {
                ops.add(getOperation(NameConverter.convertToCamelCase(entry.getKey()), null, opEntry));
            }
        }
        addChildAddOperations(ops, resourceRegistration);
        return ops.toArray(new OpenMBeanOperationInfo[ops.size()]);
    }

    private void addChildAddOperations(List<OpenMBeanOperationInfo> ops, ImmutableManagementResourceRegistration resourceRegistration) {
        for (Map.Entry<PathElement, ChildAddOperationEntry> entry : ChildAddOperationFinder.findAddChildOperations(resourceRegistration).entrySet()) {
            OpenMBeanParameterInfo addWildcardChildName = null;
            if (entry.getValue().getElement().isWildcard()) {
                addWildcardChildName = new OpenMBeanParameterInfoSupport("name", "The name of the " + entry.getValue().getElement().getKey() + " to add.", SimpleType.STRING);
            }

            ops.add(getOperation(NameConverter.createValidAddOperationName(entry.getKey()), addWildcardChildName, entry.getValue().getOperationEntry()));
        }
    }

    private OpenMBeanOperationInfo getOperation(String name, OpenMBeanParameterInfo addWildcardChildName, OperationEntry entry) {
        ModelNode opNode = entry.getDescriptionProvider().getModelDescription(null);
        OpenMBeanParameterInfo[] params = getParameterInfos(opNode);
        if (addWildcardChildName != null) {
            OpenMBeanParameterInfo[] newParams = new OpenMBeanParameterInfo[params.length + 1];
            newParams[0] = addWildcardChildName;
            System.arraycopy(params, 0, newParams, 1, params.length);
            params = newParams;
        }
        return new OpenMBeanOperationInfoSupport(
                name,
                getDescription(opNode),
                params,
                getReturnType(opNode),
                MBeanOperationInfo.UNKNOWN,
                createOperationDescriptor());
    }

    private OpenMBeanParameterInfo[] getParameterInfos(ModelNode opNode) {
        if (!opNode.hasDefined(REQUEST_PROPERTIES)) {
            return EMPTY_PARAMETERS;
        }
        List<Property> propertyList = opNode.get(REQUEST_PROPERTIES).asPropertyList();
        List<OpenMBeanParameterInfo> params = new ArrayList<OpenMBeanParameterInfo>(propertyList.size());

        for (Property prop : propertyList) {
            ModelNode value = prop.getValue();
            String paramName = NameConverter.convertToCamelCase(prop.getName());

            Map<String, Object> descriptions = new HashMap<String, Object>(4);

            boolean expressionsAllowed = prop.getValue().hasDefined(EXPRESSIONS_ALLOWED) && prop.getValue().get(EXPRESSIONS_ALLOWED).asBoolean();
            descriptions.put(DESC_EXPRESSIONS_ALLOWED, String.valueOf(expressionsAllowed));

            if (!expressionsAllowed) {
                Object defaultValue = getIfExists(value, DEFAULT);
                descriptions.put(DEFAULT_VALUE_FIELD, defaultValue);
                if (value.has(ALLOWED)) {
                    List<ModelNode> allowed = value.get(ALLOWED).asList();
                    descriptions.put(LEGAL_VALUES_FIELD,fromModelNodes(value, allowed));
                } else {
                    if (value.has(MIN)) {
                        descriptions.put(MIN_VALUE_FIELD, getIfExistsAsComparable(value, MIN));
                    }
                    if (value.has(MAX)) {
                        descriptions.put(MAX_VALUE_FIELD, getIfExistsAsComparable(value, MAX));
                    }
                }
            }


            params.add(
                    new OpenMBeanParameterInfoSupport(
                            paramName,
                            getDescription(prop.getValue()),
                            converters.convertToMBeanType(value),
                            new ImmutableDescriptor(descriptions)));

        }
        return params.toArray(new OpenMBeanParameterInfo[params.size()]);
    }

    private Set<?> fromModelNodes(final ModelNode parentNode, final List<ModelNode> nodes) {
        Set<Object> values = new HashSet<Object>(nodes.size());
        for (ModelNode node : nodes) {
            values.add(converters.fromModelNode(parentNode, node));
        }
        return values;
    }

    private Object getIfExists(final ModelNode parentNode, final String name) {
        if (parentNode.has(name)) {
            ModelNode defaultNode = parentNode.get(name);
            return converters.fromModelNode(parentNode, defaultNode);
        } else {
            return null;
        }
    }

    private Comparable<?> getIfExistsAsComparable(final ModelNode parentNode, final String name) {
        if (parentNode.has(name)) {
            ModelNode defaultNode = parentNode.get(name);
            Object value = converters.fromModelNode(parentNode, defaultNode);
            if (value instanceof Comparable) {
                return (Comparable<?>) value;
            }
        }
        return null;
    }

    private OpenType<?> getReturnType(ModelNode opNode) {
        if (!opNode.hasDefined(REPLY_PROPERTIES)) {
            return SimpleType.VOID;
        }
        if (opNode.get(REPLY_PROPERTIES).asList().size() == 0) {
            return SimpleType.VOID;
        }

        //TODO might have more than one REPLY_PROPERTIES?
        ModelNode reply = opNode.get(REPLY_PROPERTIES);
        return converters.convertToMBeanType(reply);
    }

    private MBeanNotificationInfo[] getNotifications() {
        //TODO handle notifications?
        return null;
    }

    private Descriptor createMBeanDescriptor() {
        Map<String, String> descriptions = new HashMap<String, String>();
        addMBeanExpressionSupport(descriptions);
        return new ImmutableDescriptor(descriptions);
    }

    private Descriptor createAttributeDescriptor(ModelNode attribute) {
        Map<String, String> descriptions = new HashMap<String, String>();
        addMBeanExpressionSupport(descriptions);
        Boolean allowExpressions = attribute.hasDefined(EXPRESSIONS_ALLOWED) && attribute.get(EXPRESSIONS_ALLOWED).asBoolean();
        descriptions.put(DESC_EXPRESSIONS_ALLOWED, allowExpressions.toString());
        descriptions.put(DESC_EXPRESSIONS_ALLOWED_DESC, allowExpressions ?
                MESSAGES.descriptorAttributeExpressionsAllowedTrue() : MESSAGES.descriptorAttributeExpressionsAllowedFalse());
        return new ImmutableDescriptor(descriptions);
    }

    private Descriptor createOperationDescriptor() {
        Map<String, String> descriptions = new HashMap<String, String>();
        addMBeanExpressionSupport(descriptions);
        return new ImmutableDescriptor(descriptions);
    }

    private Descriptor createParameterDescriptor(ModelNode parameter) {
        Map<String, String> descriptions = new HashMap<String, String>();
        addMBeanExpressionSupport(descriptions);
        Boolean allowExpressions = parameter.hasDefined(EXPRESSIONS_ALLOWED) && parameter.get(EXPRESSIONS_ALLOWED).asBoolean();
        descriptions.put(DESC_EXPRESSIONS_ALLOWED, allowExpressions.toString());
        descriptions.put(DESC_EXPRESSIONS_ALLOWED_DESC, allowExpressions ?
                MESSAGES.descriptorParameterExpressionsAllowedTrue() : MESSAGES.descriptorParameterExpressionsAllowedFalse());
        return new ImmutableDescriptor(descriptions);
    }


    private void addMBeanExpressionSupport(Map<String, String> descriptions) {
        if (legacy) {
            descriptions.put(DESC_MBEAN_EXPR, "true");
            descriptions.put(DESC_MBEAN_EXPR_DESCR, MESSAGES.descriptorMBeanExpressionSupportFalse());
            if (configuredDomains.getExprDomain() != null) {
                ObjectName alternate = configuredDomains.getMirroredObjectName(name);
                descriptions.put(DESC_ALTERNATE_MBEAN, alternate.toString());
                descriptions.put(DESC_ALTERNATE_MBEAN_DESCR, MESSAGES.descriptorAlternateMBeanExpressions(alternate));
            }
        } else {
            descriptions.put(DESC_MBEAN_EXPR, "false");
            descriptions.put(DESC_MBEAN_EXPR_DESCR, MESSAGES.descriptorMBeanExpressionSupportTrue());
            if (configuredDomains.getLegacyDomain() != null) {
                ObjectName alternate = configuredDomains.getMirroredObjectName(name);
                descriptions.put(DESC_ALTERNATE_MBEAN, alternate.toString());
                descriptions.put(DESC_ALTERNATE_MBEAN_DESCR, MESSAGES.descriptorAlternateMBeanLegacy(alternate));
            }
        }
    }


}
