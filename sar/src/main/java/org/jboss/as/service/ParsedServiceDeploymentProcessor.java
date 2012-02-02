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
import java.beans.PropertyEditorManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig.Inject;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig.ValueFactory;
import org.jboss.as.service.descriptor.JBossServiceAttributeConfig.ValueFactoryParameter;
import org.jboss.as.service.descriptor.JBossServiceConfig;
import org.jboss.as.service.descriptor.JBossServiceConstructorConfig;
import org.jboss.as.service.descriptor.JBossServiceConstructorConfig.Argument;
import org.jboss.as.service.descriptor.JBossServiceDependencyConfig;
import org.jboss.as.service.descriptor.JBossServiceXmlDescriptor;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MethodInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.MethodValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * DeploymentUnit processor responsible for taking JBossServiceXmlDescriptor configuration and creating the
 * corresponding services.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ParsedServiceDeploymentProcessor implements DeploymentUnitProcessor {

    /**
     * Process a deployment for JbossService configuration.  Will install a {@code JBossService} for each configured service.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
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
            throw SarMessages.MESSAGES.failedToGetAttachment("module", deploymentUnit);

        // assert reflection index
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);
        if (reflectionIndex == null)
            throw SarMessages.MESSAGES.failedToGetAttachment("reflection index", deploymentUnit);

        // install services
        final ClassLoader classLoader = module.getClassLoader();
        final List<JBossServiceConfig> serviceConfigs = serviceXmlDescriptor.getServiceConfigs();
        final ServiceTarget target = phaseContext.getServiceTarget();
        for (final JBossServiceConfig serviceConfig : serviceConfigs) {
            addServices(target, serviceConfig, classLoader, reflectionIndex);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    private void addServices(final ServiceTarget target, final JBossServiceConfig mBeanConfig, final ClassLoader classLoader, final DeploymentReflectionIndex index) throws DeploymentUnitProcessingException {
        final String mBeanClassName = mBeanConfig.getCode();
        final List<ClassReflectionIndex<?>> mBeanClassHierarchy = ReflectionUtils.getClassHierarchy(mBeanClassName, index, classLoader);
        final Object mBeanInstance = newInstance(mBeanConfig, mBeanClassHierarchy, classLoader);
        final String mBeanName = mBeanConfig.getName();
        final MBeanServices mBeanServices = new MBeanServices(mBeanName, mBeanInstance, mBeanClassHierarchy, target);

        final JBossServiceDependencyConfig[] dependencyConfigs = mBeanConfig.getDependencyConfigs();
        if (dependencyConfigs != null) {
            final Service<Object> createDestroyService = mBeanServices.getCreateDestroyService();
            for (final JBossServiceDependencyConfig dependencyConfig : dependencyConfigs) {
                final Injector<Object> injector = getInjector(dependencyConfig, mBeanClassHierarchy, createDestroyService);
                mBeanServices.addDependency(dependencyConfig.getDependencyName(), injector);
            }
        }

        final JBossServiceAttributeConfig[] attributeConfigs = mBeanConfig.getAttributeConfigs();
        if (attributeConfigs != null) {
            final Service<Object> createDestroyService = mBeanServices.getCreateDestroyService();
            for (final JBossServiceAttributeConfig attributeConfig : attributeConfigs) {
                final String propertyName = attributeConfig.getName();
                final Inject injectConfig = attributeConfig.getInject();
                final ValueFactory valueFactoryConfig = attributeConfig.getValueFactory();

                if (injectConfig != null) {
                    final Value<?> value = getValue(injectConfig, mBeanClassHierarchy);
                    final Injector<Object> injector = getPropertyInjector(propertyName, mBeanClassHierarchy, createDestroyService, value);
                    mBeanServices.addDependency(injectConfig.getBeanName(), injector);
                } else if (valueFactoryConfig != null) {
                    final Value<?> value = getValue(valueFactoryConfig, mBeanClassHierarchy, classLoader);
                    final Injector<Object> injector = getPropertyInjector(propertyName, mBeanClassHierarchy, createDestroyService, value);
                    mBeanServices.addDependency(valueFactoryConfig.getBeanName(), injector);
                } else {
                    final Value<?> value = getValue(attributeConfig, mBeanClassHierarchy);
                    final Injector<Object> injector = getPropertyInjector(propertyName, mBeanClassHierarchy, createDestroyService, Values.injectedValue());
                    mBeanServices.addInjectionValue(injector, value);
                }
            }
        }

        // register all mBean related services
        mBeanServices.install();
    }

    private static Injector<Object> getInjector(final JBossServiceDependencyConfig dependencyConfig, final List<ClassReflectionIndex<?>> mBeanClassHierarchy, final Service<Object> service) {
        final String attrName = dependencyConfig.getOptionalAttributeName();
        return attrName != null ? getPropertyInjector(attrName, mBeanClassHierarchy, service, Values.injectedValue()) : NullInjector.getInstance();
    }

    private static Value<?> getValue(final Inject injectConfig, final List<ClassReflectionIndex<?>> mBeanClassHierarchy) {
        final String propertyName = injectConfig.getPropertyName();
        Value<?> valueToInject = Values.injectedValue();
        if (propertyName != null) {
            final Method getter = ReflectionUtils.getGetter(mBeanClassHierarchy, propertyName);
            final Value<Method> getterValue = new ImmediateValue<Method>(getter);
            valueToInject = cached(new MethodValue<Object>(getterValue, valueToInject, Values.<Object>emptyList()));
        }
        return valueToInject;
    }

    private static Value<?> getValue(final ValueFactory valueFactory, final List<ClassReflectionIndex<?>> mBeanClassHierarchy, final ClassLoader classLoader) throws DeploymentUnitProcessingException {
        final String methodName = valueFactory.getMethodName();
        final ValueFactoryParameter[] parameters = valueFactory.getParameters();
        final List<Class<?>> paramTypes = new ArrayList<Class<?>>(parameters.length);
        final List<Value<?>> paramValues = new ArrayList<Value<?>>(parameters.length);
        for (ValueFactoryParameter parameter : parameters) {
            final Class<?> attributeTypeValue = ReflectionUtils.getClass(parameter.getType(), classLoader);
            paramTypes.add(attributeTypeValue);
            paramValues.add(new ImmediateValue<Object>(newValue(attributeTypeValue, parameter.getValue())));
        }
        final Value<Method> methodValue = new ImmediateValue<Method>(ReflectionUtils.getMethod(mBeanClassHierarchy, methodName, paramTypes.toArray(new Class<?>[0]), true));
        return cached(new MethodValue<Object>(methodValue, Values.injectedValue(), paramValues));
    }

    private static Value<?> getValue(final JBossServiceAttributeConfig attributeConfig, final List<ClassReflectionIndex<?>> mBeanClassHierarchy) {
        final String attributeName = attributeConfig.getName();
        final Method setterMethod = ReflectionUtils.getSetter(mBeanClassHierarchy, attributeName);
        final Class<?> setterType = setterMethod.getParameterTypes()[0];

        return new ImmediateValue<Object>(newValue(setterType, attributeConfig.getValue()));
    }

    private static Object newInstance(final JBossServiceConfig serviceConfig, final List<ClassReflectionIndex<?>> mBeanClassHierarchy, final ClassLoader classLoader) throws DeploymentUnitProcessingException {
        final JBossServiceConstructorConfig constructorConfig = serviceConfig.getConstructorConfig();
        final int paramCount = constructorConfig != null ? constructorConfig.getArguments().length : 0;
        final Class<?>[] types = new Class<?>[paramCount];
        final Object[] params = new Object[paramCount];

        if (constructorConfig != null) {
            final Argument[] arguments = constructorConfig.getArguments();
            for (int i = 0; i < paramCount; i++) {
                final Argument argument = arguments[i];
                types[i] = ReflectionUtils.getClass(argument.getType(), classLoader);
                params[i] = newValue(ReflectionUtils.getClass(argument.getType(), classLoader), argument.getValue());
            }
        }

        final Constructor<?> constructor = mBeanClassHierarchy.get(0).getConstructor(types);
        final Object mBeanInstance = ReflectionUtils.newInstance(constructor, params);

        return mBeanInstance;
    }

    private static Injector<Object> getPropertyInjector(final String propertyName, final List<ClassReflectionIndex<?>> mBeanClassHierarchy, final Service<?> service, final Value<?> value) {
        final Method setterMethod = ReflectionUtils.getSetter(mBeanClassHierarchy, propertyName);
        return new MethodInjector<Object>(setterMethod, service, Values.nullValue(), Collections.singletonList(value));
    }

    private static Object newValue(final Class<?> type, final String value) {
        final PropertyEditor editor = PropertyEditorManager.findEditor(type);
        if (editor == null) {
            SarLogger.DEPLOYMENT_SERVICE_LOGGER.propertyNotFound(type);
            return null;
        }
        editor.setAsText(value);

        return editor.getValue();
    }

}
