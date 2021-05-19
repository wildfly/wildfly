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

import java.beans.PropertyEditor;
import java.util.Collections;
import java.util.LinkedList;
import java.util.function.Supplier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.StandardMBean;

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
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
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
        final List<ClassReflectionIndex> mBeanClassHierarchy = getClassHierarchy(mBeanClassName, index, classLoader);
        final Object mBeanInstance = newInstance(mBeanConfig, mBeanClassHierarchy, classLoader);
        final String mBeanName = mBeanConfig.getName();
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final MBeanServices mBeanServices = new MBeanServices(mBeanName, mBeanInstance, mBeanClassHierarchy, target, componentInstantiator, deploymentUnit.getAttachmentList(org.jboss.as.ee.component.Attachments.WEB_SETUP_ACTIONS), classLoader, mbeanServerServiceName);

        final JBossServiceDependencyConfig[] dependencyConfigs = mBeanConfig.getDependencyConfigs();
        addDependencies(dependencyConfigs, mBeanClassHierarchy, mBeanServices, mBeanInstance);

        final JBossServiceDependencyListConfig[] dependencyListConfigs = mBeanConfig.getDependencyConfigLists();
        addDependencyLists(dependencyListConfigs, mBeanClassHierarchy, mBeanServices, mBeanInstance);


        final JBossServiceAttributeConfig[] attributeConfigs = mBeanConfig.getAttributeConfigs();
        addAttributes(attributeConfigs, mBeanClassHierarchy, mBeanServices, classLoader, mBeanInstance);

        // register all mBean related services
        mBeanServices.install();
    }

    private void addDependencies(final JBossServiceDependencyConfig[] dependencyConfigs, final List<ClassReflectionIndex> mBeanClassHierarchy, final MBeanServices mBeanServices, final Object mBeanInstance) throws DeploymentUnitProcessingException {
        if (dependencyConfigs != null) {
            for (final JBossServiceDependencyConfig dependencyConfig : dependencyConfigs) {
                final String optionalAttributeName = dependencyConfig.getOptionalAttributeName();
                if(optionalAttributeName != null){
                    final Method setter = ReflectionUtils.getSetter(mBeanClassHierarchy, optionalAttributeName);
                    final ObjectName dependencyObjectName = createDependencyObjectName(dependencyConfig.getDependencyName());
                    final Supplier<Object> objectSupplier = new ObjectSupplier(dependencyObjectName);
                    mBeanServices.addValue(setter, objectSupplier);
                }
                mBeanServices.addDependency(dependencyConfig.getDependencyName());
            }
        }
    }

    private void addDependencyLists(final JBossServiceDependencyListConfig[] dependencyListConfigs, final List<ClassReflectionIndex> mBeanClassHierarchy, final MBeanServices mBeanServices, final Object mBeanInstance) throws DeploymentUnitProcessingException {
        if(dependencyListConfigs != null){
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
                    final Method setter = ReflectionUtils.getSetter(mBeanClassHierarchy, optionalAttributeName);
                    final ObjectSupplier objectSupplier = new ObjectSupplier(dependencyObjectNames);
                    mBeanServices.addValue(setter, objectSupplier);
                }
            }
        }
    }

    private void addAttributes(final JBossServiceAttributeConfig[] attributeConfigs, final List<ClassReflectionIndex> mBeanClassHierarchy, final MBeanServices mBeanServices, final ClassLoader classLoader, final Object mBeanInstance) throws DeploymentUnitProcessingException {
        if (attributeConfigs != null) {
            for (final JBossServiceAttributeConfig attributeConfig : attributeConfigs) {
                final String propertyName = attributeConfig.getName();
                final Inject injectConfig = attributeConfig.getInject();
                final ValueFactory valueFactoryConfig = attributeConfig.getValueFactory();
                final Method setter = ReflectionUtils.getSetter(mBeanClassHierarchy, propertyName);

                if (injectConfig != null) {
                    final DelegatingSupplier propertySupplier = getObjectSupplier(injectConfig);
                    mBeanServices.addAttribute(injectConfig.getBeanName(), setter, propertySupplier);
                } else if (valueFactoryConfig != null) {
                    final DelegatingSupplier valueFactorySupplier = getObjectSupplier(valueFactoryConfig, classLoader);
                    mBeanServices.addAttribute(valueFactoryConfig.getBeanName(), setter, valueFactorySupplier);
                } else {
                    final Supplier<Object> value = getObjectSupplier(attributeConfig, mBeanClassHierarchy);
                    mBeanServices.addValue(setter, value);
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

    private static DelegatingSupplier getObjectSupplier(final Inject injectConfig) {
        return new PropertySupplier(injectConfig.getPropertyName());
    }

    private static DelegatingSupplier getObjectSupplier(final ValueFactory valueFactory, final ClassLoader classLoader) {
        final String methodName = valueFactory.getMethodName();
        final ValueFactoryParameter[] parameters = valueFactory.getParameters();
        final Class<?>[] paramTypes = new Class<?>[parameters.length];
        final Object[] args = new Object[parameters.length];
        int index = 0;
        for (ValueFactoryParameter parameter : parameters) {
            final Class<?> attributeType = ReflectionUtils.getClass(parameter.getType(), classLoader);
            paramTypes[index] = attributeType;
            args[index] = newValue(attributeType, parameter.getValue());
            index++;
        }
        return new ValueFactorySupplier(methodName, paramTypes, args);
    }

    private static Supplier<Object> getObjectSupplier(final JBossServiceAttributeConfig attributeConfig, final List<ClassReflectionIndex> mBeanClassHierarchy) {
        final String attributeName = attributeConfig.getName();
        final Method setterMethod = ReflectionUtils.getSetter(mBeanClassHierarchy, attributeName);
        final Class<?> setterType = setterMethod.getParameterTypes()[0];
        return new ObjectSupplier(newValue(setterType, attributeConfig.getValue()));
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

    private static Object newValue(final Class<?> type, final String value) {
        final PropertyEditor editor = PropertyEditorFinder.getInstance().find(type);
        if (editor == null) {
            SarLogger.ROOT_LOGGER.propertyNotFound(type);
            return null;
        }
        editor.setAsText(value);

        return editor.getValue();
    }

    /**
     * Uses the provided DeploymentReflectionIndex to provide an reflection index for the class hierarchy for
     * the class with the given name. Superclass data will not be provided for java.lang.Object or for
     * javax.management.NotificationBroadcasterSupport or javax.management.StandardMBean as those classes do not
     * provide methods relevant to our use of superclass information from the reflection index. Not providing an
     * index for those classes avoids the need to include otherwise unneeded --add-opens calls when lauching the server.
     *
     * @param className the name of the initial class in the hierarchy
     * @param index DeploymentReflectionIndex to use for creating the ClassReflectionIndex elements
     * @param classLoader classloader to use to load {@code className}
     * @return reflection indices for the class hierarchy. The first element in the returned list will be for the
     *         provided className; later elements will be for superclasses.
     */
    private static List<ClassReflectionIndex> getClassHierarchy(final String className, final DeploymentReflectionIndex index, final ClassLoader classLoader) {
        final List<ClassReflectionIndex> retVal = new LinkedList<ClassReflectionIndex>();

        Class<?> initialClazz = ReflectionUtils.getClass(className, classLoader);
        Class<?> temp = initialClazz;
        while (temp != null
                && (temp == initialClazz
                || (temp != Object.class && temp != NotificationBroadcasterSupport.class && temp != StandardMBean.class))) {
            retVal.add(index.getClassIndex(temp));
            temp = temp.getSuperclass();
        }

        return Collections.unmodifiableList(retVal);
    }

}
