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
class ModelControllerMBeanHelper {

    static final String CLASS_NAME = ModelController.class.getName();
    private final ModelController controller;

    ModelControllerMBeanHelper(ModelController controller) {
        this.controller = controller;
    }

    int getMBeanCount() {
        return new RootResourceIterator<Integer>(getRootResourceAndRegistration().getResource(), new ResourceAction<Integer>() {
            int count;
            public void onResource(PathAddress address) {
                count++;
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
            public void onResource(PathAddress address) {
                ObjectName resourceName = ObjectNameAddressUtil.createObjectName(address);
                if (name != null && !name.apply(resourceName)) {
                    return;
                }
                //TODO check query
                set.add(new ObjectInstance(resourceName, CLASS_NAME));
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
            public void onResource(PathAddress address) {
                ObjectName resourceName = ObjectNameAddressUtil.createObjectName(address);
                if (name != null && !name.apply(resourceName)) {
                    return;
                }
                //TODO check query
                set.add(resourceName);

            }

            @Override
            public Set<ObjectName> getResult() {
                return set;
            }
        }).iterate();
    }


    PathAddress resolvePathAddress(final ObjectName name) {
        return ObjectNameAddressUtil.resolvePathAddress(getRootResourceAndRegistration().getResource(), name);
    }


    MBeanInfo getMBeanInfo(final ObjectName name) throws InstanceNotFoundException {
        ResourceAndRegistration reg = getRootResourceAndRegistration();
        PathAddress address = ObjectNameAddressUtil.resolvePathAddress(reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }

        return MBeanInfoFactory.createMBeanInfo(address, getMBeanRegistration(address, reg));
    }

    Object getAttribute(final ObjectName name, final String attribute)  throws AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = ObjectNameAddressUtil.resolvePathAddress(reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }
        return getAttribute(reg, address, name, attribute);
    }

    AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = ObjectNameAddressUtil.resolvePathAddress(reg.getResource(), name);
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
            throw new InstanceNotFoundException("No description provider found for " + address);
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

        return TypeConverter.fromModelNode(description.require(ATTRIBUTES).require(attributeName), result.get(RESULT));
    }


    void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = ObjectNameAddressUtil.resolvePathAddress(reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }
        setAttribute(reg, address, name, attribute);

    }

    AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = ObjectNameAddressUtil.resolvePathAddress(reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }

        for (Attribute attribute : attributes.asList()) {
            try {
                setAttribute(reg, address, name, attribute);
            } catch (Exception e) {
                throw new ReflectionException(e, "Could not set " + attribute.getName());
            }
        }

        return attributes;
    }

    private void setAttribute(final ResourceAndRegistration reg, final PathAddress address, final ObjectName name, final Attribute attribute)  throws InvalidAttributeValueException, AttributeNotFoundException, InstanceNotFoundException {
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);
        final DescriptionProvider provider = registration.getModelDescription(PathAddress.EMPTY_ADDRESS);
        if (provider == null) {
            throw new InstanceNotFoundException("No description provider found for " + address);
        }
        final ModelNode description = provider.getModelDescription(null);
        final String attributeName = findAttributeName(description.get(ATTRIBUTES), attribute.getName());

        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).set(address.toModelNode());
        op.get(NAME).set(attributeName);
        try {
            op.get(VALUE).set(TypeConverter.toModelNode(description.require(ATTRIBUTES).require(attributeName), attribute.getValue()));
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new InvalidAttributeValueException("Bad type for '" + attribute.getName() + "'");
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
            throw new IllegalArgumentException("Null operation name");
        }
        if (params == null) {
            params = new Object[0];
        }
        if (signature == null) {
            signature = new String[0];
        }
        if (params.length != signature.length) {
            throw new IllegalArgumentException("params and signature have different lengths");
        }

        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        PathAddress address = ObjectNameAddressUtil.resolvePathAddress(reg.getResource(), name);
        if (address == null) {
            throw createInstanceNotFoundException(name);
        }
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);

        String realOperationName = null;
        DescriptionProvider provider = registration.getOperationDescription(PathAddress.EMPTY_ADDRESS, operationName);

        if (provider != null) {
            realOperationName = operationName;
        } else {
            Map<String, OperationEntry> ops = registration.getOperationDescriptions(PathAddress.EMPTY_ADDRESS, false);
            for (Map.Entry<String, OperationEntry> entry : ops.entrySet()) {
                if (operationName.equals(NameConverter.convertToCamelCase(entry.getKey()))) {
                    provider = entry.getValue().getDescriptionProvider();
                    realOperationName = entry.getKey();
                    break;
                }
            }
        }

        if (provider == null) {
            ChildAddOperationEntry entry = ChildAddOperationFinder.findAddChildOperation(reg.getRegistration().getSubModel(address), operationName);
            if (entry == null) {
                throw new MBeanException(null, "No operation called '" + operationName + "' at " + address);
            }
            PathElement element = entry.getElement();
            if (element.isWildcard()) {
                if (params.length == 0) {
                    throw new IllegalStateException("Need the name parameter for wildcard add");
                }
                element = PathElement.pathElement(element.getKey(), (String)params[0]);
                Object[] newParams = new Object[params.length - 1];
                System.arraycopy(params, 1, newParams, 0, newParams.length);
                params = newParams;
            }
            return invoke(entry.getDescriptionProvider(), ADD, address.append(element), params);
        }
        return invoke(provider, realOperationName, address, params);
    }

    private Object invoke(final DescriptionProvider provider, final String operationName, PathAddress address, Object[] params)  throws InstanceNotFoundException, MBeanException, ReflectionException {

        final ModelNode description = provider.getModelDescription(null);
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        op.get(OP_ADDR).set(address.toModelNode());
        if (params.length > 0) {
            ModelNode requestProperties = description.require(REQUEST_PROPERTIES);
            Set<String> keys = requestProperties.keys();
            if (keys.size() != params.length) {
                throw new IllegalArgumentException("params have a different length than the description");
            }
            Iterator<String> it = requestProperties.keys().iterator();
            for (int i = 0 ; i < params.length ; i++) {
                String attributeName = it.next();
                ModelNode paramDescription = requestProperties.get(attributeName);
                op.get(attributeName).set(TypeConverter.toModelNode(paramDescription, params[i]));
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
        return TypeConverter.fromModelNode(description.get(REPLY_PROPERTIES), result.get(RESULT));
    }

    static InstanceNotFoundException createInstanceNotFoundException(ObjectName name) {
        return new InstanceNotFoundException("No MBean found with name " + name);
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
            throw new InstanceNotFoundException("No registration found for path address " + address);
        }
        return resourceRegistration;
    }

    private String getFailureDescription(ModelNode result) {
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            return result.get(FAILURE_DESCRIPTION).asString();
        }
        return null;
    }

    private String findAttributeName(ModelNode attributes, String attributeName) {
        if (attributes.hasDefined(attributeName)) {
            return attributeName;
        }
        for (String key : attributes.keys()) {
            if (NameConverter.convertToCamelCase(key).equals(attributeName)) {
                return key;
            }
        }
        throw new IllegalArgumentException("Could not find any attribute matching: " + attributeName);
    }
}
