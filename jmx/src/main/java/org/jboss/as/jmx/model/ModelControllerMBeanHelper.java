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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.jmx.model.ChildAddOperationFinder.ChildAddOperationEntry;
import org.jboss.as.jmx.model.RootResourceIterator.ResourceAction;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.as.server.operations.RootResourceHack.ResourceAndRegistration;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelControllerMBeanHelper {

    static final String CLASS_NAME = ModelController.class.getName();
    private final boolean standalone;
    private final ModelController controller;
    private final PathAddress CORE_SERVICE_PLATFORM_MBEAN = PathAddress.pathAddress(PathElement.pathElement("core-service", "platform-mbean"));

    private final TypeConverters converters;
    private final ConfiguredDomains configuredDomains;
    private final String domain;

    ModelControllerMBeanHelper(TypeConverters converters, ConfiguredDomains configuredDomains, String domain, ModelController controller) {
        this.converters = converters;
        this.configuredDomains = configuredDomains;
        this.domain = domain;
        this.controller = controller;

        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).setEmptyList();
        op.get(NAME).set("launch-type");
        final ModelNode result = execute(op);
        String error = getFailureDescription(result);
        if (error != null) {
            throw new IllegalStateException(error);
        }
        standalone = result.require(RESULT).asString().equals("STANDALONE");
    }

    int getMBeanCount() {
        return new RootResourceIterator<Integer>(getRootResourceAndRegistration().getResource(), new ResourceAction<Integer>() {
            int count;
            public boolean onResource(PathAddress address) {
                if (isExcludeAddress(address)) {
                    return false;
                }
                count++;
                return true;
            }

            public Integer getResult() {
                return count;
            }
        }).iterate();
    }

    Set<ObjectInstance> queryMBeans(final ObjectName name, final QueryExp query) {
        return new RootResourceIterator<Set<ObjectInstance>>(getRootResourceAndRegistration().getResource(), new ResourceAction<Set<ObjectInstance>>() {
            Set<ObjectInstance> set = new HashSet<ObjectInstance>();

            @Override
            public boolean onResource(PathAddress address) {
                if (isExcludeAddress(address)) {
                    return false;
                }
                ObjectName resourceName = ObjectNameAddressUtil.createObjectName(domain, address);
                if (name == null || name.apply(resourceName)) {
                    //TODO check query
                    set.add(new ObjectInstance(resourceName, CLASS_NAME));
                }
                return true;
            }

            @Override
            public Set<ObjectInstance> getResult() {
                return set;
            }
        }).iterate();
    }

    Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) {
        return new RootResourceIterator<Set<ObjectName>>(getRootResourceAndRegistration().getResource(), new ResourceAction<Set<ObjectName>>() {
            Set<ObjectName> set = new HashSet<ObjectName>();

            @Override
            public boolean onResource(PathAddress address) {
                if (isExcludeAddress(address)) {
                    return false;
                }
                ObjectName resourceName = ObjectNameAddressUtil.createObjectName(domain, address);
                if (name == null || name.apply(resourceName)) {
                    //TODO check query
                    set.add(resourceName);
                }
                return true;
            }

            @Override
            public Set<ObjectName> getResult() {
                return set;
            }
        }).iterate();
    }


    PathAddress resolvePathAddress(final ObjectName name) {
        return ObjectNameAddressUtil.resolvePathAddress(domain, getRootResourceAndRegistration().getResource(), name);
    }


    MBeanInfo getMBeanInfo(final ObjectName name) throws InstanceNotFoundException {
        ResourceAndRegistration reg = getRootResourceAndRegistration();
        PathAddress address = ObjectNameAddressUtil.resolvePathAddress(domain, reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }

        return MBeanInfoFactory.createMBeanInfo(name, converters, configuredDomains, standalone, address, getMBeanRegistration(address, reg));
    }

    Object getAttribute(final ObjectName name, final String attribute)  throws AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = ObjectNameAddressUtil.resolvePathAddress(domain, reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }
        return getAttribute(reg, address, name, attribute);
    }

    AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = ObjectNameAddressUtil.resolvePathAddress(domain, reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }
        AttributeList list = new AttributeList();
        for (String attribute : attributes) {
            try {
                list.add(new Attribute(attribute, getAttribute(reg, address, name, attribute)));
            } catch (AttributeNotFoundException e) {
                throw new ReflectionException(e);
            }
        }
        return list;
    }

    private Object getAttribute(final ResourceAndRegistration reg, final PathAddress address, final ObjectName name, final String attribute)  throws ReflectionException, AttributeNotFoundException, InstanceNotFoundException {
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);
        final DescriptionProvider provider = registration.getModelDescription(PathAddress.EMPTY_ADDRESS);
        if (provider == null) {
            throw MESSAGES.descriptionProviderNotFound(address);
        }
        final ModelNode description = provider.getModelDescription(null);
        final String attributeName = findAttributeName(description.get(ATTRIBUTES), attribute);

        ModelNode op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(address.toModelNode());
        op.get(NAME).set(attributeName);
        ModelNode result = execute(op);
        String error = getFailureDescription(result);
        if (error != null) {
            throw new AttributeNotFoundException(error);
        }

        return converters.fromModelNode(description.require(ATTRIBUTES).require(attributeName), result.get(RESULT));
    }


    void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = ObjectNameAddressUtil.resolvePathAddress(domain, reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }
        setAttribute(reg, address, name, attribute);

    }

    AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = ObjectNameAddressUtil.resolvePathAddress(domain, reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }

        for (Attribute attribute : attributes.asList()) {
            try {
                setAttribute(reg, address, name, attribute);
            } catch (Exception e) {
                throw MESSAGES.cannotSetAttribute(e, attribute.getName());
            }
        }

        return attributes;
    }

    private void setAttribute(final ResourceAndRegistration reg, final PathAddress address, final ObjectName name, final Attribute attribute)  throws InvalidAttributeValueException, AttributeNotFoundException, InstanceNotFoundException {
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);
        final DescriptionProvider provider = registration.getModelDescription(PathAddress.EMPTY_ADDRESS);
        if (provider == null) {
            throw MESSAGES.descriptionProviderNotFound(address);
        }
        final ModelNode description = provider.getModelDescription(null);
        final String attributeName = findAttributeName(description.get(ATTRIBUTES), attribute.getName());

        if (!standalone) {
            throw MESSAGES.attributeNotWritable(attribute);
        }

        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(address.toModelNode());
        op.get(NAME).set(attributeName);
        try {
            op.get(VALUE).set(converters.toModelNode(description.require(ATTRIBUTES).require(attributeName), attribute.getValue()));
        } catch (ClassCastException e) {
            throw MESSAGES.invalidAttributeType(e, attribute.getName());
        }
        ModelNode result = execute(op);
        String error = getFailureDescription(result);
        if (error != null) {
            throw new InvalidAttributeValueException(error);
        }
    }

    ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        if (resolvePathAddress(name) == null) {
            throw createInstanceNotFoundException(name);
        }
        return new ObjectInstance(name, CLASS_NAME);
    }

    Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (operationName == null) {
            throw MESSAGES.nullVar("operationName");
        }
        if (params == null) {
            params = new Object[0];
        }
        if (signature == null) {
            signature = new String[0];
        }
        if (params.length != signature.length) {
            throw MESSAGES.differentLengths("params", "signature");
        }

        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        PathAddress address = ObjectNameAddressUtil.resolvePathAddress(domain, reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);

        String realOperationName = null;
        OperationEntry opEntry = registration.getOperationEntry(PathAddress.EMPTY_ADDRESS, operationName);
        if (opEntry != null) {
            realOperationName = operationName;
        } else {
            String opName = NameConverter.convertFromCamelCase(operationName);
            opEntry = registration.getOperationEntry(PathAddress.EMPTY_ADDRESS, opName);
            if (opEntry != null) {
                realOperationName = opName;
            }
        }

        if (opEntry == null) {
            //Brute force search in case the operation name is not standard format
            Map<String, OperationEntry> ops = registration.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, false);
            for (Map.Entry<String, OperationEntry> entry : ops.entrySet()) {
                if (operationName.equals(NameConverter.convertToCamelCase(entry.getKey()))) {
                    opEntry = entry.getValue();
                    realOperationName = entry.getKey();
                    break;
                }
            }
        }

        if (opEntry == null) {
            ChildAddOperationEntry entry = ChildAddOperationFinder.findAddChildOperation(reg.getRegistration().getSubModel(address), operationName);
            if (entry == null) {
                throw MESSAGES.noOperationCalled(null, operationName, address);
            }
            PathElement element = entry.getElement();
            if (element.isWildcard()) {
                if (params.length == 0) {
                    throw MESSAGES.wildcardNameParameterRequired();
                }
                element = PathElement.pathElement(element.getKey(), (String)params[0]);
                Object[] newParams = new Object[params.length - 1];
                System.arraycopy(params, 1, newParams, 0, newParams.length);
                params = newParams;
            }
            return invoke(entry.getOperationEntry(), ADD, address.append(element), params);
        }
        return invoke(opEntry, realOperationName, address, params);
    }

    private Object invoke(final OperationEntry entry, final String operationName, PathAddress address, Object[] params)  throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (!standalone && !entry.getFlags().contains(OperationEntry.Flag.READ_ONLY)) {
            throw MESSAGES.noOperationCalled(operationName);
        }

        final ModelNode description = entry.getDescriptionProvider().getModelDescription(null);
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        op.get(OP_ADDR).set(address.toModelNode());
        if (params.length > 0) {
            ModelNode requestProperties = description.require(REQUEST_PROPERTIES);
            Set<String> keys = requestProperties.keys();
            if (keys.size() != params.length) {
                throw MESSAGES.differentLengths("params", "description");
            }
            Iterator<String> it = requestProperties.keys().iterator();
            for (int i = 0 ; i < params.length ; i++) {
                String attributeName = it.next();
                ModelNode paramDescription = requestProperties.get(attributeName);
                op.get(attributeName).set(converters.toModelNode(paramDescription, params[i]));
            }
        }

        ModelNode result = execute(op);
        String error = getFailureDescription(result);
        if (error != null) {
            throw new ReflectionException(null, error);
        }

        if (!description.hasDefined(REPLY_PROPERTIES)) {
            return null;
        }
        //TODO we could have more than one reply property
        return converters.fromModelNode(description.get(REPLY_PROPERTIES), result.get(RESULT));
    }

    static InstanceNotFoundException createInstanceNotFoundException(ObjectName name) {
        return MESSAGES.mbeanNotFound(name);
    }

    private ResourceAndRegistration getRootResourceAndRegistration() {
        return RootResourceHack.INSTANCE.getRootResource(controller);
    }

    private ModelNode execute(ModelNode op) {
        return controller.execute(op, null, OperationTransactionControl.COMMIT, null);
    }

    private ImmutableManagementResourceRegistration getMBeanRegistration(PathAddress address, ResourceAndRegistration reg) throws InstanceNotFoundException {
        //TODO Populate MBeanInfo
        ImmutableManagementResourceRegistration resourceRegistration = reg.getRegistration().getSubModel(address);
        if (resourceRegistration == null) {
            throw MESSAGES.registrationNotFound(address);
        }
        return resourceRegistration;
    }

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).toString();
        }
        return null;
    }

    private String findAttributeName(ModelNode attributes, String attributeName) throws AttributeNotFoundException{
        if (attributes.hasDefined(attributeName)) {
            return attributeName;
        }
        for (String key : attributes.keys()) {
            if (NameConverter.convertToCamelCase(key).equals(attributeName)) {
                return key;
            }
        }
        throw MESSAGES.attributeNotFound(attributeName);
    }

    private boolean isExcludeAddress(PathAddress pathAddress) {
        return pathAddress.equals(CORE_SERVICE_PLATFORM_MBEAN);
    }

    public static ObjectName createRootObjectName(String domain) {
        try {
            return ObjectName.getInstance(domain, "management-root", "server");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }
}
