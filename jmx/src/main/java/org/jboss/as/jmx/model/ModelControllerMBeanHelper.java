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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_MECHANISM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.jmx.JmxMessages.MESSAGES;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMRuntimeException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.jmx.model.ChildAddOperationFinder.ChildAddOperationEntry;
import org.jboss.as.jmx.model.ResourceAccessControlUtil.ResourceAccessControl;
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
    private static final String AUTHORIZED_ERROR = "JBAS013456";

    private final boolean standalone;
    private final ModelController controller;
    private final ResourceAccessControlUtil accessControlUtil;
    private final PathAddress CORE_SERVICE_PLATFORM_MBEAN = PathAddress.pathAddress(PathElement.pathElement("core-service", "platform-mbean"));

    private final TypeConverters converters;
    private final ConfiguredDomains configuredDomains;
    private final String domain;

    ModelControllerMBeanHelper(TypeConverters converters, ConfiguredDomains configuredDomains, String domain,
                               ModelController controller, boolean standalone) {
        this.converters = converters;
        this.configuredDomains = configuredDomains;
        this.domain = domain;
        this.controller = controller;
        this.accessControlUtil = new ResourceAccessControlUtil(controller);
        this.standalone = standalone;
    }

    int getMBeanCount() {
        return new RootResourceIterator<Integer>(accessControlUtil, getRootResourceAndRegistration().getResource(), new ResourceAction<Integer>() {
            int count;

            @Override
            public ObjectName onAddress(PathAddress address) {
                return isExcludeAddress(address) ? null : ObjectNameAddressUtil.createObjectName(domain, address);
            }

            public boolean onResource(ObjectName address) {
                count++;
                return true;
            }

            public Integer getResult() {
                return count;
            }
        }).iterate();
    }

    Set<ObjectInstance> queryMBeans(final ObjectName name, final QueryExp query) {
        return new RootResourceIterator<Set<ObjectInstance>>(accessControlUtil, getRootResourceAndRegistration().getResource(),
                new ObjectNameMatchResourceAction<Set<ObjectInstance>>(name) {

            Set<ObjectInstance> set = new HashSet<ObjectInstance>();

            @Override
            public boolean onResource(ObjectName resourceName) {
                if (name == null || name.apply(resourceName)) {
                    //TODO check query
                    set.add(new ObjectInstance(resourceName, CLASS_NAME));
                }
                return true;
            }

            @Override
            public Set<ObjectInstance> getResult() {
                if (set.size() == 1 && set.contains(ModelControllerMBeanHelper.createRootObjectName(domain))) {
                    return Collections.emptySet();
                }
                return set;
            }
        }).iterate();
    }

    Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) {
        return new RootResourceIterator<Set<ObjectName>>(accessControlUtil, getRootResourceAndRegistration().getResource(),
                new ObjectNameMatchResourceAction<Set<ObjectName>>(name) {

            Set<ObjectName> set = new HashSet<ObjectName>();

            @Override
            public boolean onResource(ObjectName resourceName) {
                if (name == null || name.apply(resourceName)) {
                    //TODO check query
                    set.add(resourceName);
                }
                return true;
            }

            @Override
            public Set<ObjectName> getResult() {
                if (set.size() == 1 && set.contains(ModelControllerMBeanHelper.createRootObjectName(domain))) {
                  return Collections.emptySet();
                }
                return set;
            }
        }).iterate();
    }


    PathAddress resolvePathAddress(final ObjectName name) {
        return ObjectNameAddressUtil.resolvePathAddress(domain, getRootResourceAndRegistration().getResource(), name);
    }

    PathAddress resolvePathAddress(final ObjectName name, ResourceAndRegistration reg) {
        return ObjectNameAddressUtil.resolvePathAddress(domain, reg.getResource(), name);
    }

    MBeanInfo getMBeanInfo(final ObjectName name) throws InstanceNotFoundException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw MESSAGES.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, true);
        return MBeanInfoFactory.createMBeanInfo(name, converters, configuredDomains, standalone, address, getMBeanRegistration(address, reg));
    }

    Object getAttribute(final ObjectName name, final String attribute)  throws AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw MESSAGES.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);
        return getAttribute(reg, address, name, attribute, accessControl);
    }

    AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw MESSAGES.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);
        AttributeList list = new AttributeList();
        for (String attribute : attributes) {
            try {
                list.add(new Attribute(attribute, getAttribute(reg, address, name, attribute, accessControl)));
            } catch (AttributeNotFoundException e) {
                throw new ReflectionException(e);
            }
        }
        return list;
    }

    private Object getAttribute(final ResourceAndRegistration reg, final PathAddress address, final ObjectName name, final String attribute, final ResourceAccessControl accessControl)  throws ReflectionException, AttributeNotFoundException, InstanceNotFoundException {
        final ImmutableManagementResourceRegistration registration = getMBeanRegistration(address, reg);
        final DescriptionProvider provider = registration.getModelDescription(PathAddress.EMPTY_ADDRESS);
        if (provider == null) {
            throw MESSAGES.descriptionProviderNotFound(address);
        }
        final ModelNode description = provider.getModelDescription(null);
        final String attributeName = findAttributeName(description.get(ATTRIBUTES), attribute);

        if (!accessControl.isReadableAttribute(attributeName)) {
            throw MESSAGES.notAuthorizedToReadAttribute(attributeName);
        }


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
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw MESSAGES.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);
        setAttribute(reg, address, name, attribute, accessControl);

    }

    AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        final ResourceAndRegistration reg = getRootResourceAndRegistration();
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw MESSAGES.mbeanNotFound(name);
        }
        final ResourceAccessControl accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);

        for (Attribute attribute : attributes.asList()) {
            try {
                setAttribute(reg, address, name, attribute, accessControl);
            } catch (JMRuntimeException e) {
                //Propagate the JMRuntimeException thrown from authorization
                throw e;
            } catch (Exception e) {
                throw MESSAGES.cannotSetAttribute(e, attribute.getName());
            }
        }

        return attributes;
    }

    private void setAttribute(final ResourceAndRegistration reg, final PathAddress address, final ObjectName name, final Attribute attribute, ResourceAccessControl accessControl)  throws InvalidAttributeValueException, AttributeNotFoundException, InstanceNotFoundException {
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

        if (!accessControl.isWritableAttribute(attributeName)) {
            throw MESSAGES.notAuthorizedToWriteAttribute(attributeName);
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
            //Since read-resource-description does not know the parameters of the operation, i.e. if a vault expression is used or not,
            //check the error code
            //TODO add a separate authorize step where we check ourselves that the operation will pass authorization?
            if (isVaultExpression(attribute.getValue()) && error.contains(AUTHORIZED_ERROR)) {
                throw MESSAGES.notAuthorizedToWriteAttribute(attributeName);
            }
            throw new InvalidAttributeValueException(error);
        }
    }

    ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        final PathAddress address = resolvePathAddress(name);
        if (address == null) {
            throw MESSAGES.mbeanNotFound(name);
        }
        accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(name, address, false);
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
        final PathAddress address = resolvePathAddress(name, reg);
        if (address == null) {
            throw MESSAGES.mbeanNotFound(name);
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

        ResourceAccessControl accessControl;
        if (operationName.equals("add")) {
            accessControl = accessControlUtil.getResourceAccess(address, true);
        } else {
            ObjectName objectName = ObjectNameAddressUtil.createObjectName(operationName, address);
            accessControl = accessControlUtil.getResourceAccessWithInstanceNotFoundExceptionIfNotAccessible(
                    objectName, address, true);
        }

        if (!accessControl.isExecutableOperation(operationName)) {
            throw MESSAGES.notAuthorizedToExecuteOperation(operationName);
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
            if (error.contains(AUTHORIZED_ERROR)) {
                for (Object param : params) {
                    //Since read-resource-description does not know the parameters of the operation, i.e. if a vault expression is used or not,
                    //check the error code
                    //TODO add a separate authorize step where we check ourselves that the operation will pass authorization?
                    if (isVaultExpression(param)) {
                        throw MESSAGES.notAuthorizedToExecuteOperation(operationName);
                    }
                }
            }
            throw new ReflectionException(null, error);
        }

        if (!description.hasDefined(REPLY_PROPERTIES)) {
            return null;
        }
        //TODO we could have more than one reply property
        return converters.fromModelNode(description.get(REPLY_PROPERTIES), result.get(RESULT));
    }

    private ResourceAndRegistration getRootResourceAndRegistration() {
        return RootResourceHack.INSTANCE.getRootResource(controller);
    }

    private ModelNode execute(ModelNode op) {
        op.get(OPERATION_HEADERS, ACCESS_MECHANISM).set(AccessMechanism.JMX.toString());
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

    private boolean isVaultExpression(Object value) {
        if (value != null && value.getClass() == String.class){
            String valueString = (String)value;
            if (ExpressionResolver.EXPRESSION_PATTERN.matcher(valueString).matches()) {
                return TypeConverters.VAULT_PATTERN.matcher(valueString).matches();
            }

        }
        return false;
    }

    public static ObjectName createRootObjectName(String domain) {
        try {
            return ObjectName.getInstance(domain, "management-root", "server");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private abstract class ObjectNameMatchResourceAction<T> implements ResourceAction<T> {

        private final ObjectName baseName;
        private final Map<String, String> properties;
        private final ObjectName domainOnlyName;

        protected ObjectNameMatchResourceAction(ObjectName baseName) {
            this.baseName = baseName;
            this.properties = baseName == null ? Collections.<String, String>emptyMap() : baseName.getKeyPropertyList();
            try {
                this.domainOnlyName = baseName == null ? null : ObjectName.getInstance(baseName.getDomain() + ":*");
            } catch (MalformedObjectNameException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public ObjectName onAddress(PathAddress address) {
            if (isExcludeAddress(address)) {
                return null;
            }

            ObjectName result = null;
            ObjectName toMatch = ObjectNameAddressUtil.createObjectName(domain, address);
            if (baseName == null) {
                result = toMatch;
            } else if (address.size() == 0) {
                // We can't compare the ObjectName properties a la the final 'else' block,
                // because the special management=server property will not match
                // Just confirm correct domain
                if (domainOnlyName.apply(toMatch)) {
                    result = toMatch;
                }
            } else if (address.size() >= properties.size()) {
                // We have same or more elements than our target has properties; let it do the match
                if (baseName.apply(toMatch)) {
                    result = toMatch;
                }
            } else {
                // Address may be a parent of an interesting address, so see if it matches all elements it has
                boolean matches = domainOnlyName.apply(toMatch);
                if (matches) {
                    for (Map.Entry<String, String> entry : toMatch.getKeyPropertyList().entrySet()) {

                        String propertyValue = properties.get(entry.getKey());
                        if (propertyValue == null
                                || (!entry.getValue().equals(propertyValue))
                                        && !baseName.isPropertyValuePattern(entry.getKey())) {
                            matches = false;
                            break;
                        }
                    }
                }
                if (matches) {
                    result = toMatch;
                }
            }
            return result;
        }
    }
}
