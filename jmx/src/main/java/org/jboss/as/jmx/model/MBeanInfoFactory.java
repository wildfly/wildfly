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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
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

    private static final OpenMBeanParameterInfo[] EMPTY_PARAMETERS = new OpenMBeanParameterInfo[0];
    private final boolean standalone;
    private final ImmutableManagementResourceRegistration resourceRegistration;
    private final ModelNode providedDescription;
    private final PathAddress pathAddress;

    private MBeanInfoFactory(final boolean standalone, final PathAddress address, final ImmutableManagementResourceRegistration resourceRegistration) {
        this.standalone = standalone;
        this.resourceRegistration = resourceRegistration;
        DescriptionProvider provider = resourceRegistration.getModelDescription(PathAddress.EMPTY_ADDRESS);
        providedDescription = provider != null ? provider.getModelDescription(null) : new ModelNode();
        this.pathAddress = address;
    }

    private static MBeanInfoFactory createFactory(final boolean standalone, final PathAddress address, final ImmutableManagementResourceRegistration resourceRegistration) throws InstanceNotFoundException{
        return new MBeanInfoFactory(standalone, address, resourceRegistration);
    }

    static MBeanInfo createMBeanInfo(final boolean standalone, final PathAddress address, final ImmutableManagementResourceRegistration resourceRegistration) throws InstanceNotFoundException{
        return createFactory(standalone, address, resourceRegistration).createMBeanInfo();
    }

    private MBeanInfo createMBeanInfo() {
        return new OpenMBeanInfoSupport(ModelControllerMBeanHelper.CLASS_NAME,
                getDescription(providedDescription),
                getAttributes(),
                getConstructors(),
                getOperations(),
                getNotifications());
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
                TypeConverter.convertToMBeanType(attribute),
                true,
                writable,
                false);
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
                MBeanOperationInfo.UNKNOWN);
    }

    private OpenMBeanParameterInfo[] getParameterInfos(ModelNode opNode) {
        if (!opNode.hasDefined(REQUEST_PROPERTIES)) {
            return EMPTY_PARAMETERS;
        }
        List<OpenMBeanParameterInfo> params = new ArrayList<OpenMBeanParameterInfo>();
        for (Property prop : opNode.get(REQUEST_PROPERTIES).asPropertyList()) {
            ModelNode value = prop.getValue();
            final String paramName = NameConverter.convertToCamelCase(prop.getName());
            params.add(
                    new OpenMBeanParameterInfoSupport(
                            paramName,
                            getDescription(prop.getValue()),
                            TypeConverter.convertToMBeanType(value)));
        }
        return params.toArray(new OpenMBeanParameterInfo[params.size()]);
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
        return TypeConverter.convertToMBeanType(reply);
    }

    private MBeanNotificationInfo[] getNotifications() {
        //TODO handle notifications?
        return null;
    }
}
