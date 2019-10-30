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
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.spi.ObjectFactory;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ExternalContextObjectFactory;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.context.external.ExternalContexts;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.ExternalContextBinderService;
import org.jboss.as.naming.service.ExternalContextsService;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.msc.service.ServiceBuilder;
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
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();

        installRuntimeServices(context, name, model);
    }

    void installRuntimeServices(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {
        boolean allowed = false;
        for (String ns : GLOBAL_NAMESPACES) {
            if (name.startsWith(ns)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw NamingLogger.ROOT_LOGGER.invalidNamespaceForBinding(name, Arrays.toString(GLOBAL_NAMESPACES));
        }

        final BindingType type = BindingType.forName(NamingBindingResourceDefinition.BINDING_TYPE.resolveModelAttribute(context, model).asString());
        if (type == BindingType.SIMPLE) {
            installSimpleBinding(context, name, model);
        } else if (type == BindingType.OBJECT_FACTORY) {
            installObjectFactory(context, name, model);
        } else if (type == BindingType.LOOKUP) {
            installLookup(context, name, model);
        } else if (type == BindingType.EXTERNAL_CONTEXT) {
            installExternalContext(context, name, model);
        } else {
            throw NamingLogger.ROOT_LOGGER.unknownBindingType(type.toString());
        }
    }

    void installSimpleBinding(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        Object bindValue = createSimpleBinding(context, model);

        ValueManagedReferenceFactory referenceFactory = new ValueManagedReferenceFactory(new ImmediateValue<Object>(bindValue));


        final BinderService binderService = new BinderService(name, bindValue);
        binderService.getManagedObjectInjector().inject(new MutableManagedReferenceFactory(referenceFactory));
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);

        final ServiceTarget serviceTarget = context.getServiceTarget();
        ServiceBuilder<ManagedReferenceFactory> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());

        builder.install();
    }

    private Object createSimpleBinding(OperationContext context, ModelNode model) throws OperationFailedException {
        final String value = NamingBindingResourceDefinition.VALUE.resolveModelAttribute(context, model).asString();
        final String type;
        if (model.hasDefined(TYPE)) {
            type = NamingBindingResourceDefinition.TYPE.resolveModelAttribute(context, model).asString();
        } else {
            type = null;
        }

        return coerceToType(value, type);
    }


    void installObjectFactory(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final ObjectFactory objectFactoryClassInstance = createObjectFactory(context, model);

        final Hashtable<String, String> environment = getObjectFactoryEnvironment(context, model);
        ContextListAndJndiViewManagedReferenceFactory factory = new ObjectFactoryManagedReference(objectFactoryClassInstance, name, environment);

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);


        final BinderService binderService = new BinderService(name, objectFactoryClassInstance);
        binderService.getManagedObjectInjector().inject(new MutableManagedReferenceFactory(factory));

        serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .install();
    }

    private ObjectFactory createObjectFactory(OperationContext context, ModelNode model) throws OperationFailedException {
        final ModuleIdentifier moduleID = ModuleIdentifier.fromString(NamingBindingResourceDefinition.MODULE.resolveModelAttribute(context, model).asString());
        final String className = NamingBindingResourceDefinition.CLASS.resolveModelAttribute(context, model).asString();
        final Module module;
        try {
            module = Module.getBootModuleLoader().loadModule(moduleID);
        } catch (ModuleNotFoundException e) {
            throw NamingLogger.ROOT_LOGGER.moduleNotFound(moduleID, e.getMessage());
        } catch (ModuleLoadException e) {
            throw NamingLogger.ROOT_LOGGER.couldNotLoadModule(moduleID);
        }

        final ObjectFactory objectFactoryClassInstance;

        final ClassLoader cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            final Class<?> clazz = module.getClassLoader().loadClass(className);
            objectFactoryClassInstance = (ObjectFactory) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw NamingLogger.ROOT_LOGGER.couldNotLoadClassFromModule(className, moduleID);
        } catch (InstantiationException e) {
            throw NamingLogger.ROOT_LOGGER.couldNotInstantiateClassInstanceFromModule(className, moduleID);
        } catch (IllegalAccessException e) {
            throw NamingLogger.ROOT_LOGGER.couldNotInstantiateClassInstanceFromModule(className, moduleID);
        } catch (ClassCastException e) {
            throw NamingLogger.ROOT_LOGGER.notAnInstanceOfObjectFactory(className, moduleID);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }
        return objectFactoryClassInstance;
    }


    void installExternalContext(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

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

        final ExternalContextBinderService binderService = new ExternalContextBinderService(name, objectFactoryClassInstance);
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

        serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addDependency(ExternalContextsService.SERVICE_NAME, ExternalContexts.class, binderService.getExternalContextsInjector())
                .install();
    }


    private Hashtable<String, String> getObjectFactoryEnvironment(OperationContext context, ModelNode model) throws OperationFailedException {
        Hashtable<String, String> environment;
        Map<String, String> resolvedModelAttribute = NamingBindingResourceDefinition.ENVIRONMENT.unwrap(context, model);
        environment = new Hashtable<String, String>(resolvedModelAttribute);
        return environment;
    }

    void installLookup(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final String lookup = NamingBindingResourceDefinition.LOOKUP.resolveModelAttribute(context, model).asString();

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);

        final BinderService binderService = new BinderService(name);
        binderService.getManagedObjectInjector().inject(new MutableManagedReferenceFactory(new LookupManagedReferenceFactory(lookup)));

        serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .install();
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
                throw NamingLogger.ROOT_LOGGER.unableToTransformURLBindingValue(value, e);
            }
        } else {
            throw NamingLogger.ROOT_LOGGER.unsupportedSimpleBindingType(type);
        }

    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.populateModel(context, operation, resource);
        context.addStep(NamingBindingResourceDefinition.VALIDATE_RESOURCE_MODEL_OPERATION_STEP_HANDLER, OperationContext.Stage.MODEL);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : NamingBindingResourceDefinition.ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }
    }

    void doRebind(OperationContext context, ModelNode model, BinderService service) throws OperationFailedException {
        ManagedReferenceFactory ref = service.getManagedObjectInjector().getValue();
        if(ref instanceof MutableManagedReferenceFactory) {
            MutableManagedReferenceFactory factory = (MutableManagedReferenceFactory) ref;
            final BindingType type = BindingType.forName(NamingBindingResourceDefinition.BINDING_TYPE.resolveModelAttribute(context, model).asString());
            if (type == BindingType.SIMPLE) {
                Object bindValue = createSimpleBinding(context, model);
                factory.setFactory(new ValueManagedReferenceFactory(new ImmediateValue<Object>(bindValue)));
                service.setSource(bindValue);
            } else if (type == BindingType.OBJECT_FACTORY) {
                final ObjectFactory objectFactoryClassInstance = createObjectFactory(context, model);
                final Hashtable<String, String> environment = getObjectFactoryEnvironment(context, model);
                factory.setFactory(new ObjectFactoryManagedReference(objectFactoryClassInstance, service.getName(), environment));
                service.setSource(objectFactoryClassInstance);
            } else if (type == BindingType.LOOKUP) {
                final String lookup = NamingBindingResourceDefinition.LOOKUP.resolveModelAttribute(context, model).asString();
                factory.setFactory(new LookupManagedReferenceFactory(lookup));
                service.setSource(null);
            } else if (type == BindingType.EXTERNAL_CONTEXT) {
                throw NamingLogger.ROOT_LOGGER.cannotRebindExternalContext();
            } else {
                throw NamingLogger.ROOT_LOGGER.unknownBindingType(type.toString());
            }
        } else {
            throw NamingLogger.ROOT_LOGGER.cannotRebindExternalContext();
        }

    }


    static class MutableManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {

        MutableManagedReferenceFactory(ManagedReferenceFactory factory) {
            this.factory = factory;
        }

        private volatile ManagedReferenceFactory factory;

        @Override
        public ManagedReference getReference() {
            return factory.getReference();
        }

        public ManagedReferenceFactory getFactory() {
            return factory;
        }

        public void setFactory(ManagedReferenceFactory factory) {
            this.factory = factory;
        }

        @Override
        public String getInstanceClassName() {
            if(factory instanceof ContextListManagedReferenceFactory) {
                return ((ContextListManagedReferenceFactory) factory).getInstanceClassName();
            }
            ManagedReference ref = getReference();
            try {
                final Object value = ref.getInstance();
                return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
            } finally {
                ref.release();
            }
        }

        @Override
        public String getJndiViewInstanceValue() {
            if(factory instanceof ContextListAndJndiViewManagedReferenceFactory) {
                return ((ContextListAndJndiViewManagedReferenceFactory) factory).getJndiViewInstanceValue();
            }
            ManagedReference reference = getReference();
            try {
                return String.valueOf(reference.getInstance());
            } finally {
                reference.release();
            }
        }
    }

    private static class ObjectFactoryManagedReference implements ContextListAndJndiViewManagedReferenceFactory {
        private final ObjectFactory objectFactoryClassInstance;
        private final String name;
        private final Hashtable<String, String> environment;

        ObjectFactoryManagedReference(ObjectFactory objectFactoryClassInstance, String name, Hashtable<String, String> environment) {
            this.objectFactoryClassInstance = objectFactoryClassInstance;
            this.name = name;
            this.environment = environment;
        }

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
    }

    private static class LookupManagedReferenceFactory implements ManagedReferenceFactory {
        private final String lookup;

        LookupManagedReferenceFactory(String lookup) {
            this.lookup = lookup;
        }

        @Override
        public ManagedReference getReference() {
            try {
                final Object value = new InitialContext().lookup(lookup);
                return new ImmediateManagedReference(value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
