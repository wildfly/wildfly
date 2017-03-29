/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.service;

import static org.jboss.msc.value.Values.cached;

import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.service.component.ServiceComponentInstantiator;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig.Inject;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig.ValueFactory;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig.ValueFactoryParameter;
import org.jboss.as.service.descriptor.JBossServiceConfig;
import org.jboss.as.service.descriptor.JBossServiceConstructorConfig;
import org.jboss.as.service.descriptor.JBossServiceConstructorConfig.Argument;
import org.jboss.as.service.descriptor.JBossServiceDependencyConfig;
import org.jboss.as.service.descriptor.JBossServiceDependencyListConfig;
import org.jboss.as.service.descriptor.JBossServiceXmlDescriptor;
import org.jboss.as.service.logging.SarLogger;
import org.jboss.common.beans.property.finder.PropertyEditorFinder;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MethodInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.MethodValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * DeploymentUnit processor responsible for taking JBossServiceXmlDescriptor configuration and creating the
 * corresponding services.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Eduardo Martins
 */
public class ParsedServiceDeploymentProcessor implements DeploymentUnitProcessor {

    private final ServiceName mbeanServerServiceName;

    ParsedServiceDeploymentProcessor(ServiceName mbeanServerServiceName) {

        this.mbeanServerServiceName = mbeanServerServiceName;
    }

    /**
     * Process a deployment for JbossService configuration.  Will install a {@code JBossService} for each configured service.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final JBossServiceXmlDescriptor serviceXmlDescriptor = deploymentUnit.getAttachment(JBossServiceXmlDescriptor.ATTACHMENT_KEY);
        if (serviceXmlDescriptor == null) {
            // Skip deployments without a service xml descriptor
            return;
        }

        // assert module
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null)
            throw SarLogger.ROOT_LOGGER.failedToGetAttachment("module", deploymentUnit);

        // assert reflection index
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);
        if (reflectionIndex == null)
            throw SarLogger.ROOT_LOGGER.failedToGetAttachment("reflection index", deploymentUnit);

        // install services
        final ClassLoader classLoader = module.getClassLoader();
        final List<JBossServiceConfig> serviceConfigs = serviceXmlDescriptor.getServiceConfigs();
        final ServiceTarget target = phaseContext.getServiceTarget();
        final Map<String,ServiceComponentInstantiator> serviceComponents = deploymentUnit.getAttachment(ServiceAttachments.SERVICE_COMPONENT_INSTANTIATORS);
        for (final JBossServiceConfig serviceConfig : serviceConfigs) {
            addServices(target, serviceConfig, classLoader, reflectionIndex, serviceComponents != null ? serviceComponents.get(serviceConfig.getName()) : null, phaseContext);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }

    private void addServices(final ServiceTarget target, final JBossServiceConfig mBeanConfig, final ClassLoader classLoader, final DeploymentReflectionIndex index, ServiceComponentInstantiator componentInstantiator, final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final String mBeanClassName = mBeanConfig.getCode();
        final List<ClassReflectionIndex> mBeanClassHierarchy = ReflectionUtils.getClassHierarchy(mBeanClassName, index, classLoader);
        final Object mBeanInstance = newInstance(mBeanConfig, mBeanClassHierarchy, classLoader);
        final String mBeanName = mBeanConfig.getName();
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final MBeanServices mBeanServices = new MBeanServices(mBeanName, mBeanInstance, mBeanClassHierarchy, target, componentInstantiator, deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS), classLoader, mbeanServerServiceName);

        final JBossServiceDependencyConfig[] dependencyConfigs = mBeanConfig.getDependencyConfigs();
        addDependencies(dependencyConfigs, mBeanClassHierarchy, mBeanServices);

        final JBossServiceDependencyListConfig[] dependencyListConfigs = mBeanConfig.getDependencyConfigLists();
        addDependencyLists(dependencyListConfigs, mBeanClassHierarchy, mBeanServices);


        final JBossServiceAttributeConfig[] attributeConfigs = mBeanConfig.getAttributeConfigs();
        addAttributes(attributeConfigs, mBeanClassHierarchy, mBeanServices, classLoader);

        // register all mBean related services
        mBeanServices.install();
    }

    private void addDependencies(final JBossServiceDependencyConfig[] dependencyConfigs, final List<ClassReflectionIndex> mBeanClassHierarchy, final MBeanServices mBeanServices) throws DeploymentUnitProcessingException {
        if (dependencyConfigs != null) {
            final Service<Object> createDestroyService = mBeanServices.getCreateDestroyService();
            for (final JBossServiceDependencyConfig dependencyConfig : dependencyConfigs) {
                final String optionalAttributeName = dependencyConfig.getOptionalAttributeName();
                if(optionalAttributeName != null){
                    final Injector<Object> injector = getOptionalAttributeInjector(optionalAttributeName, mBeanClassHierarchy, createDestroyService);
                    final ObjectName dependencyObjectName = createDependencyObjectName(dependencyConfig.getDependencyName());
                    final ImmediateValue<ObjectName> dependencyNameValue = new ImmediateValue<ObjectName>(dependencyObjectName);
                    mBeanServices.addInjectionValue(injector, dependencyNameValue);
                }
                mBeanServices.addDependency(dependencyConfig.getDependencyName());
            }
        }
    }

    private void addDependencyLists(final JBossServiceDependencyListConfig[] dependencyListConfigs, final List<ClassReflectionIndex> mBeanClassHierarchy, final MBeanServices mBeanServices) throws DeploymentUnitProcessingException {
        if(dependencyListConfigs != null){
            final Service<Object> createDestroyService = mBeanServices.getCreateDestroyService();
            for(final JBossServiceDependencyListConfig dependencyListConfig: dependencyListConfigs) {
                final List<ObjectName> dependencyObjectNames = new ArrayList<ObjectName>(dependencyListConfig.getDependencyConfigs().length);
                for(final JBossServiceDependencyConfig dependencyConfig: dependencyListConfig.getDependencyConfigs()){
                        final String dependencyName = dependencyConfig.getDependencyName();
                        mBeanServices.addDependency(dependencyName);
                        final ObjectName dependencyObjectName = createDependencyObjectName(dependencyName);
                        dependencyObjectNames.add(dependencyObjectName);
                }
                final String optionalAttributeName = dependencyListConfig.getOptionalAttributeName();
                if(optionalAttributeName != null){
                    final Injector<Object> injector = getOptionalAttributeInjector(optionalAttributeName, mBeanClassHierarchy, createDestroyService);
                    final ImmediateValue<List<ObjectName>> dependencyNamesValue = new ImmediateValue<List<ObjectName>>(dependencyObjectNames);
                    mBeanServices.addInjectionValue(injector, dependencyNamesValue);
                }
            }
        }
    }

    private void addAttributes(final JBossServiceAttributeConfig[] attributeConfigs, final List<ClassReflectionIndex> mBeanClassHierarchy, final MBeanServices mBeanServices, final ClassLoader classLoader) throws DeploymentUnitProcessingException {
        if (attributeConfigs != null) {
            final Service<Object> createDestroyService = mBeanServices.getCreateDestroyService();
            for (final JBossServiceAttributeConfig attributeConfig : attributeConfigs) {
                final String propertyName = attributeConfig.getName();
                final Inject injectConfig = attributeConfig.getInject();
                final ValueFactory valueFactoryConfig = attributeConfig.getValueFactory();

                if (injectConfig != null) {
                    final Value<?> value = getValue(injectConfig);
                    final Injector<Object> injector = getPropertyInjector(propertyName, mBeanClassHierarchy, createDestroyService, value);
                    mBeanServices.addAttribute(injectConfig.getBeanName(), injector);
                } else if (valueFactoryConfig != null) {
                    final Value<?> value = getValue(valueFactoryConfig, classLoader);
                    final Injector<Object> injector = getPropertyInjector(propertyName, mBeanClassHierarchy, createDestroyService, value);
                    mBeanServices.addAttribute(valueFactoryConfig.getBeanName(), injector);
                } else {
                    final Value<?> value = getValue(attributeConfig, mBeanClassHierarchy);
                    final Injector<Object> injector = getPropertyInjector(propertyName, mBeanClassHierarchy, createDestroyService, Values.injectedValue());
                    mBeanServices.addInjectionValue(injector, value);
                }
            }
        }
    }

    private ObjectName createDependencyObjectName(final String dependencyName) throws DeploymentUnitProcessingException {
        try {
            return new ObjectName(dependencyName);
        } catch(MalformedObjectNameException exception){
            throw SarLogger.ROOT_LOGGER.malformedDependencyName(exception, dependencyName);
        }
    }

    private static Injector<Object> getOptionalAttributeInjector(final String attributeName, final List<ClassReflectionIndex> mBeanClassHierarchy, final Service<Object> service) {
        return getPropertyInjector(attributeName, mBeanClassHierarchy, service, Values.injectedValue());
    }

    private static Value<?> getValue(final Inject injectConfig) {
        final String propertyName = injectConfig.getPropertyName();
        Value<?> valueToInject = Values.injectedValue();
        if (propertyName != null) {
            final Value<Method> methodValue = new InjectedBeanMethodValue(Values.injectedValue(), new InjectedBeanMethodValue.MethodFinder() {
                @Override
                public Method find(Class<?> clazz) {
                    return ReflectionUtils.getGetter(clazz, propertyName);
                }
            });
            valueToInject = cached(new MethodValue<Object>(methodValue, valueToInject, Values.<Object>emptyList()));
        }
        return valueToInject;
    }

    private static Value<?> getValue(final ValueFactory valueFactory, final ClassLoader classLoader) throws DeploymentUnitProcessingException {
        final String methodName = valueFactory.getMethodName();
        final ValueFactoryParameter[] parameters = valueFactory.getParameters();
        final List<Class<?>> paramTypes = new ArrayList<Class<?>>(parameters.length);
        final List<Value<?>> paramValues = new ArrayList<Value<?>>(parameters.length);
        for (ValueFactoryParameter parameter : parameters) {
            final Class<?> attributeTypeValue = ReflectionUtils.getClass(parameter.getType(), classLoader);
            paramTypes.add(attributeTypeValue);
            paramValues.add(new ImmediateValue<Object>(newValue(attributeTypeValue, parameter.getValue())));
        }
        final Value<Method> methodValue = new InjectedBeanMethodValue(Values.injectedValue(), new InjectedBeanMethodValue.MethodFinder() {
            @Override
            public Method find(Class<?> clazz) {
                return ReflectionUtils.getMethod(clazz, methodName, paramTypes.toArray(new Class<?>[0]));
            }
        });
        return cached(new MethodValue<Object>(methodValue, Values.injectedValue(), paramValues));
    }

    private static Value<?> getValue(final JBossServiceAttributeConfig attributeConfig, final List<ClassReflectionIndex> mBeanClassHierarchy) {
        final String attributeName = attributeConfig.getName();
        final Method setterMethod = ReflectionUtils.getSetter(mBeanClassHierarchy, attributeName);
        final Class<?> setterType = setterMethod.getParameterTypes()[0];

        return new ImmediateValue<Object>(newValue(setterType, attributeConfig.getValue()));
    }

    private static Object newInstance(final JBossServiceConfig serviceConfig, final List<ClassReflectionIndex> mBeanClassHierarchy, final ClassLoader deploymentClassLoader) throws DeploymentUnitProcessingException {
        // set TCCL so that the MBean instantiation happens in the deployment's classloader
        final ClassLoader oldTCCL = WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(deploymentClassLoader);
        try {
            final JBossServiceConstructorConfig constructorConfig = serviceConfig.getConstructorConfig();
            final int paramCount = constructorConfig != null ? constructorConfig.getArguments().length : 0;
            final Class<?>[] types = new Class<?>[paramCount];
            final Object[] params = new Object[paramCount];

            if (constructorConfig != null) {
                final Argument[] arguments = constructorConfig.getArguments();
                for (int i = 0; i < paramCount; i++) {
                    final Argument argument = arguments[i];
                    types[i] = ReflectionUtils.getClass(argument.getType(), deploymentClassLoader);
                    params[i] = newValue(ReflectionUtils.getClass(argument.getType(), deploymentClassLoader), argument.getValue());
                }
            }
            final Constructor<?> constructor = mBeanClassHierarchy.get(0).getConstructor(types);
            if(constructor == null){
                throw SarLogger.ROOT_LOGGER.defaultConstructorNotFound(mBeanClassHierarchy.get(0).getIndexedClass());
            }
            final Object mBeanInstance = ReflectionUtils.newInstance(constructor, params);

            return mBeanInstance;
        } finally {
            // switch back the TCCL
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTCCL);
        }
    }

    private static Injector<Object> getPropertyInjector(final String propertyName, final List<ClassReflectionIndex> mBeanClassHierarchy, final Service<?> service, final Value<?> value) {
        final Method setterMethod = ReflectionUtils.getSetter(mBeanClassHierarchy, propertyName);
        return new MethodInjector<Object>(setterMethod, service, Values.nullValue(), Collections.singletonList(value));
    }

    private static Object newValue(final Class<?> type, final String value) {
        final PropertyEditor editor = PropertyEditorFinder.getInstance().find(type);
        if (editor == null) {
            SarLogger.ROOT_LOGGER.propertyNotFound(type);
            return null;
        }
        editor.setAsText(value);

        return editor.getValue();
    }

}
