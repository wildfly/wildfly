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

package org.jboss.as.deployment.item;

import org.jboss.as.deployment.descriptor.JBossServiceAttributeConfig;
import org.jboss.as.deployment.descriptor.JBossServiceConfig;
import org.jboss.as.deployment.descriptor.JBossServiceConstructorConfig;
import org.jboss.as.deployment.descriptor.JBossServiceDependencyConfig;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.PropertyInjector;
import org.jboss.msc.reflect.Property;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchInjectionBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ConstructedValue;
import org.jboss.msc.value.LookupClassValue;
import org.jboss.msc.value.LookupConstructorValue;
import org.jboss.msc.value.LookupPropertyValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 *  Deployment item responsible for taking a JBossServiceConfig object and creating the necessary services.
 * 
 * @author John E. Bailey
 */
public class JBossServiceDeploymentItem implements DeploymentItem {
    private static final Logger logger = Logger.getLogger("org.jboss.as.deployment.service");
    private final JBossServiceConfig serviceConfig;

    public JBossServiceDeploymentItem(JBossServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public void install(DeploymentItemContext context) {
        final Module module = context.getModule();
        final ClassLoader classLoader = module.getClassLoader();
        final Value<ClassLoader> classLoaderValue = Values.immediateValue(classLoader);
        final BatchBuilder batchBuilder = context.getBatchBuilder();

        final String codeName = serviceConfig.getCode();
        final LookupClassValue classValue = new LookupClassValue(codeName, classLoaderValue);

        final List<Value<?>> constructorArguments = new ArrayList<Value<?>>();
        final List<Value<Class<?>>> constructorSignature = new ArrayList<Value<Class<?>>>();

        final JBossServiceConstructorConfig constructorConfig = serviceConfig.getConstructorConfig();
        if(constructorConfig != null) {
            final JBossServiceConstructorConfig.Argument[] arguments = constructorConfig.getArguments();
            for(JBossServiceConstructorConfig.Argument argument : arguments) {
                final Value<Class<?>> attributeTypeValue = new LookupClassValue(argument.getType(), classLoaderValue);
                final Value<?> value = new ArgumentValue(attributeTypeValue, argument.getValue());
                constructorArguments.add(value);
                constructorSignature.add(attributeTypeValue);
            }
        }

        final LookupConstructorValue constructorValue = new LookupConstructorValue(classValue, constructorSignature);
        final ConstructedValue constructedValue = new ConstructedValue(constructorValue, constructorArguments);
        final JBossService<?> jBossService = new JBossService<Object>(constructedValue);

        final String serviceName = serviceConfig.getName();
        final BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(convert(serviceName), jBossService);
        final JBossServiceDependencyConfig[] dependencyConfigs = serviceConfig.getDependencyConfigs();
        if(dependencyConfigs != null) {
            for(JBossServiceDependencyConfig dependencyConfig : dependencyConfigs) {
                final BatchInjectionBuilder injectionBuilder = serviceBuilder.addDependency(convert(dependencyConfig.getDependencyName()));
                final String optionalAttributeName = dependencyConfig.getOptionalAttributeName();
                if(optionalAttributeName != null) {
                    injectionBuilder.toInjector(new PropertyInjector(new LookupPropertyValue(classValue, optionalAttributeName), jBossService));
                }
            }
        }

        final JBossServiceAttributeConfig[] attributeConfigs = serviceConfig.getAttributeConfigs();
        if(attributeConfigs != null) {
            for(JBossServiceAttributeConfig attributeConfig : attributeConfigs) {
                final String attributeName = attributeConfig.getName();
                final JBossServiceAttributeConfig.Inject inject = attributeConfig.getInject();
                final JBossServiceAttributeConfig.ValueFactory valueFactory = attributeConfig.getValueFactory();
                final Value<Property> propertyValue = new LookupPropertyValue(classValue, attributeName);
                final Injector<?> propInjector = new PropertyInjector(propertyValue, jBossService);
                if(inject != null) {
                    final BatchInjectionBuilder injectionBuilder = serviceBuilder.addDependency(convert(inject.getBeanName()));
                    final String propertyName = inject.getPropertyName();
                    if(propertyName != null) {
                        injectionBuilder.fromProperty(inject.getPropertyName());
                    }
                    injectionBuilder.toInjector(propInjector);
                } else if(valueFactory != null) {
                    final String methodName = valueFactory.getMethodName();
                    final JBossServiceAttributeConfig.ValueFactoryParameter[] parameters = valueFactory.getParameters();
                    final List<Value<Class<?>>> paramTypes = new ArrayList<Value<Class<?>>>(parameters.length);
                    final List<Value<?>> paramValues = new ArrayList<Value<?>>(parameters.length);
                    for(JBossServiceAttributeConfig.ValueFactoryParameter parameter : parameters) {
                        final Value<Class<?>> attributeTypeValue = new LookupClassValue(parameter.getType(), classLoaderValue);
                        paramTypes.add(attributeTypeValue);
                        final Value<?> value = new ArgumentValue(attributeTypeValue, parameter.getValue());
                        paramValues.add(value);
                    }
                    serviceBuilder.addDependency(convert(valueFactory.getBeanName()))
                        .fromMethod(methodName, paramTypes, paramValues)
                        .toInjector(propInjector);
                } else {
                    serviceBuilder.addInjectionValue(new AttributeValue(classValue, attributeName, attributeConfig.getValue()))
                        .toInjector(propInjector);
                }
            }
        }
    }

    private ServiceName convert(final String name) {
        if(name == null)
            throw new IllegalArgumentException("Name must not be null");
        return ServiceName.of(name.split("\\."));
    }

    private static class AttributeValue<T> implements Value<T> {
        private final Value<Class<?>> targetClassValue;
        private final String name;
        private final String value;

        private AttributeValue(Value<Class<?>> targetClassValue, String name, String value) {
            this.targetClassValue = targetClassValue;
            this.name = name;
            this.value = value;
        }

        @Override
        public T getValue() throws IllegalStateException {
            Class<?> type = null;
            final String expectedMethodName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            final Class<?> targetType = targetClassValue.getValue();
            final Method[] methods = targetType.getMethods();
            for(Method method : methods) {
                if(expectedMethodName.equals(method.getName())) {
                    final Class<?>[] types = method.getParameterTypes(); 
                    if(types.length == 1) {
                        type = types[0];
                        break;
                    }
                }
            }
            if(type == null) {
                logger.warn("Unable to find type for property " + name + " on class " + targetType);
                return null;
            }
            final PropertyEditor editor = PropertyEditorManager.findEditor(type);
            if(editor == null) {
                logger.warn("Unable to find PropertyEditor for type " + type);
                return null;
            }
            editor.setAsText(value);
            return (T)editor.getValue();
        }
    }

    private static class ArgumentValue<T> implements Value<T> {
        private final Value<Class<?>> typeValue;
        private final String value;

        private ArgumentValue(final Value<Class<?>> typeValue, final String value) {
            this.typeValue = typeValue;
            this.value = value;
        }

        @Override
        public T getValue() throws IllegalStateException {
            final Class<?> type = typeValue.getValue();
            final PropertyEditor editor = PropertyEditorManager.findEditor(type);
            if(editor == null) {
                logger.warn("Unable to find PropertyEditor for type " + type);
                return null;
            }
            editor.setAsText(value);
            return (T)editor.getValue();
        }
    }
}
