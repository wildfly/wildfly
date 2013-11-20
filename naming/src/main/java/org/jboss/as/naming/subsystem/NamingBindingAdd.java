/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming.subsystem;

import static org.jboss.as.naming.subsystem.NamingSubsystemModel.TYPE;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.spi.ObjectFactory;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ExternalContextObjectFactory;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingMessages;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link org.jboss.as.controller.AbstractAddStepHandler} to handle the add operation for simple JNDI bindings
 *
 * @author Stuart Douglas
 * @author Eduardo Martins
 */
public class NamingBindingAdd extends AbstractAddStepHandler {

    private static final String[] GLOBAL_NAMESPACES = {"java:global", "java:jboss", "java:/"};

    static final NamingBindingAdd INSTANCE = new NamingBindingAdd();

    private NamingBindingAdd() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();

        installRuntimeServices(context, name, model, verificationHandler, newControllers);
    }

    void installRuntimeServices(final OperationContext context, final String name, final ModelNode model, ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        boolean allowed = false;
        for (String ns : GLOBAL_NAMESPACES) {
            if (name.startsWith(ns)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw NamingMessages.MESSAGES.invalidNamespaceForBinding(name, Arrays.toString(GLOBAL_NAMESPACES));
        }

        final BindingType type = BindingType.forName(NamingBindingResourceDefinition.BINDING_TYPE.resolveModelAttribute(context, model).asString());
        if (type == BindingType.SIMPLE) {
            installSimpleBinding(context, name, model, verificationHandler, newControllers);
        } else if (type == BindingType.OBJECT_FACTORY) {
            installObjectFactory(context, name, model, verificationHandler, newControllers);
        } else if (type == BindingType.LOOKUP) {
            installLookup(context, name, model, verificationHandler, newControllers);
        } else if (type == BindingType.EXTERNAL_CONTEXT) {
            installExternalContext(context, name, model, verificationHandler, newControllers);
        } else {
            throw NamingMessages.MESSAGES.unknownBindingType(type.toString());
        }
    }

    void installSimpleBinding(final OperationContext context, final String name, final ModelNode model, ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final String value = NamingBindingResourceDefinition.VALUE.resolveModelAttribute(context, model).asString();
        final String type;
        if (model.hasDefined(TYPE)) {
            type = NamingBindingResourceDefinition.TYPE.resolveModelAttribute(context, model).asString();
        } else {
            type = null;
        }

        Object bindValue = coerceToType(value, type);

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);

        final BinderService binderService = new BinderService(name, bindValue);
        binderService.getManagedObjectInjector().inject(new ValueManagedReferenceFactory(new ImmediateValue<Object>(bindValue)));

        ServiceBuilder<ManagedReferenceFactory> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        if (newControllers != null) {
            newControllers.add(
                    builder.install());
        } else {
            builder.install();
        }
    }


    void installObjectFactory(final OperationContext context, final String name, final ModelNode model, ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final ModuleIdentifier moduleID = ModuleIdentifier.fromString(NamingBindingResourceDefinition.MODULE.resolveModelAttribute(context, model).asString());
        final String className = NamingBindingResourceDefinition.CLASS.resolveModelAttribute(context, model).asString();
        final Module module;
        try {
            module = Module.getBootModuleLoader().loadModule(moduleID);
        } catch (ModuleLoadException e) {
            throw NamingMessages.MESSAGES.couldNotLoadModule(moduleID);
        }

        final ObjectFactory objectFactoryClassInstance;

        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            final Class<?> clazz = module.getClassLoader().loadClass(className);
            objectFactoryClassInstance = (ObjectFactory) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw NamingMessages.MESSAGES.couldNotLoadClassFromModule(className, moduleID);
        } catch (InstantiationException e) {
            throw NamingMessages.MESSAGES.couldNotInstantiateClassInstanceFromModule(className, moduleID);
        } catch (IllegalAccessException e) {
            throw NamingMessages.MESSAGES.couldNotInstantiateClassInstanceFromModule(className, moduleID);
        } catch (ClassCastException e) {
            throw NamingMessages.MESSAGES.notAnInstanceOfObjectFactory(className, moduleID);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);

        final Hashtable<String, String> environment = getObjectFactoryEnvironment(context, model);

        final BinderService binderService = new BinderService(name, objectFactoryClassInstance);
        binderService.getManagedObjectInjector().inject(new ContextListAndJndiViewManagedReferenceFactory() {
            @Override
            public ManagedReference getReference() {
                try {
                    final Object value = objectFactoryClassInstance.getObjectInstance(name, null, null, environment);
                    return new ImmediateManagedReference(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String getInstanceClassName() {
                final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(objectFactoryClassInstance.getClass().getClassLoader());
                    final Object value = getReference().getInstance();
                    return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
                }
            }

            @Override
            public String getJndiViewInstanceValue() {
                final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(objectFactoryClassInstance.getClass().getClassLoader());
                    return String.valueOf(getReference().getInstance());
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
                }
            }
        });

        ServiceBuilder<ManagedReferenceFactory> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        if (newControllers != null) {
            newControllers.add(
                    builder.install());
        } else {
            builder.install();
        }
    }


    void installExternalContext(final OperationContext context, final String name, final ModelNode model, ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final String moduleID = NamingBindingResourceDefinition.MODULE.resolveModelAttribute(context, model).asString();
        final String className = NamingBindingResourceDefinition.CLASS.resolveModelAttribute(context, model).asString();
        final ModelNode cacheNode = NamingBindingResourceDefinition.CACHE.resolveModelAttribute(context, model);
        boolean cache = cacheNode.isDefined() ? cacheNode.asBoolean() : false;

        final ObjectFactory objectFactoryClassInstance = new ExternalContextObjectFactory();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);

        final Hashtable<String, String> environment = getObjectFactoryEnvironment(context, model);
        environment.put(ExternalContextObjectFactory.CACHE_CONTEXT, Boolean.toString(cache));
        environment.put(ExternalContextObjectFactory.INITIAL_CONTEXT_CLASS, className);
        environment.put(ExternalContextObjectFactory.INITIAL_CONTEXT_MODULE, moduleID);

        final BinderService binderService = new BinderService(name, objectFactoryClassInstance);
        binderService.getManagedObjectInjector().inject(new ContextListAndJndiViewManagedReferenceFactory() {
            @Override
            public ManagedReference getReference() {
                try {
                    final Object value = objectFactoryClassInstance.getObjectInstance(name, null, null, environment);
                    return new ImmediateManagedReference(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String getInstanceClassName() {
                return className;
            }

            @Override
            public String getJndiViewInstanceValue() {
                final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(objectFactoryClassInstance.getClass().getClassLoader());
                    return String.valueOf(getReference().getInstance());
                } finally {
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
                }
            }
        });

        ServiceBuilder<ManagedReferenceFactory> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        if (newControllers != null) {
            newControllers.add(
                    builder.install());
        } else {
            builder.install();
        }
    }


    private Hashtable<String, String> getObjectFactoryEnvironment(OperationContext context, ModelNode model) throws OperationFailedException {
        Hashtable<String, String> environment;
        Map<String, String> resolvedModelAttribute = NamingBindingResourceDefinition.ENVIRONMENT.unwrap(context, model);
        environment = new Hashtable<String, String>(resolvedModelAttribute);
        return environment;
    }

    void installLookup(final OperationContext context, final String name, final ModelNode model, ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final String lookup = NamingBindingResourceDefinition.LOOKUP.resolveModelAttribute(context, model).asString();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);

        final BinderService binderService = new BinderService(name);
        binderService.getManagedObjectInjector().inject(new ContextListAndJndiViewManagedReferenceFactory() {
            @Override
            public ManagedReference getReference() {
                try {
                    final Object value = new InitialContext().lookup(lookup);
                    return new ImmediateManagedReference(value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String getInstanceClassName() {
                final Object value = getReference().getInstance();
                return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
            }

            @Override
            public String getJndiViewInstanceValue() {
                return String.valueOf(getReference().getInstance());
            }
        });

        ServiceBuilder<ManagedReferenceFactory> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        if (newControllers != null) {
            newControllers.add(
                    builder.install());
        } else {
            builder.install();
        }
    }

    private Object coerceToType(final String value, final String type) throws OperationFailedException {
        if (type == null || type.isEmpty() || type.equals(String.class.getName())) {
            return value;
        } else if (type.equals("char") || type.equals("java.lang.Character")) {
            return value.charAt(0);
        } else if (type.equals("byte") || type.equals("java.lang.Byte")) {
            return Byte.parseByte(value);
        } else if (type.equals("short") || type.equals("java.lang.Short")) {
            return Short.parseShort(value);
        } else if (type.equals("int") || type.equals("java.lang.Integer")) {
            return Integer.parseInt(value);
        } else if (type.equals("long") || type.equals("java.lang.Long")) {
            return Long.parseLong(value);
        } else if (type.equals("float") || type.equals("java.lang.Float")) {
            return Float.parseFloat(value);
        } else if (type.equals("double") || type.equals("java.lang.Double")) {
            return Double.parseDouble(value);
        } else if (type.equals("boolean") || type.equals("java.lang.Boolean")) {
            return Boolean.parseBoolean(value);
        } else if (type.equals(URL.class.getName())) {
            try {
                return new URL(value);
            } catch (MalformedURLException e) {
                throw NamingMessages.MESSAGES.unableToTransformURLBindingValue(value, e);
            }
        } else {
            throw NamingMessages.MESSAGES.unsupportedSimpleBindingType(type);
        }

    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : NamingBindingResourceDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
        NamingBindingResourceDefinition.validateResourceModel(model);
    }
}
