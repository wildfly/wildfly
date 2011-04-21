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

package org.jboss.as.mc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.mc.descriptor.BeanMetaDataConfig;
import org.jboss.as.mc.descriptor.KernelDeploymentXmlDescriptor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ConstructedValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * DeploymentUnit processor responsible for taking KernelDeploymentXmlDescriptor
 * configuration and creating the corresponding services.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ParsedKernelDeploymentProcessor implements DeploymentUnitProcessor {

    /**
     * Name prefix of all MC-style beans.
     */
    public static final ServiceName JBOSS_MC_POJO = ServiceName.JBOSS.append("mc", "pojo");

    /**
     * Process a deployment for KernelDeployment configuration.
     * Will install a {@code MC bean} for each configured bean.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final KernelDeploymentXmlDescriptor kdXmlDescriptor = phaseContext.getDeploymentUnit().getAttachment(KernelDeploymentXmlDescriptor.ATTACHMENT_KEY);
        if(kdXmlDescriptor == null)
            return;

        final Module module = phaseContext.getDeploymentUnit().getAttachment(Attachments.MODULE);
        if(module == null)
            throw new DeploymentUnitProcessingException("Failed to get module attachment for " + phaseContext.getDeploymentUnit());
        final DeploymentReflectionIndex index = phaseContext.getAttachment(Attachments.REFLECTION_INDEX);
        final List<BeanMetaDataConfig> beanConfigs = kdXmlDescriptor.getBeans();
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        for(final BeanMetaDataConfig beanConfig : beanConfigs) {
            final String className = beanConfig.getBeanClass();
            try {
                addBean(serviceTarget, beanConfig, Class.forName(className, false, module.getClassLoader()), index);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Bean class " + className + " not found", e);
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    @SuppressWarnings({"unchecked"})
    private <T> void addBean(final ServiceTarget serviceTarget, BeanMetaDataConfig beanConfig, Class<T> clazz, DeploymentReflectionIndex deploymentIndex) throws ClassNotFoundException {
        final ClassReflectionIndex<T> index = deploymentIndex.getClassIndex(clazz);
        final Constructor<T> constructor = index.getConstructor(new Class[]{});
        final List<? extends Value<?>> args = Collections.emptyList();
        final ServiceName beanServiceName = JBOSS_MC_POJO.append(beanConfig.getName());
        // TODO - decide if we really need NOT_INSTALLED and DESCRIBED stages
        // INSTANTIATED stage
        final ServiceName instantiatedServiceName = beanServiceName.append(BeanState.INSTANTIATED.name());
        final ValueService<T> instantiatedService = new ValueService<T>(new ConstructedValue(constructor, args));
        final ServiceBuilder<T> instantiatedServiceBuilder = serviceTarget.addService(instantiatedServiceName, instantiatedService);
        // TODO - add declared dependencies and injections for this stage -here-
        instantiatedServiceBuilder.install();
        // CONFIGURED stage
        final ServiceName configuredServiceName = beanServiceName.append(BeanState.CONFIGURED.name());
        final ServiceBuilder<T> configuredServiceBuilder = serviceTarget.addService(configuredServiceName, new ValueService<T>(instantiatedService));
        // TODO - add declared dependencies and injections for this stage -here-
        configuredServiceBuilder.addDependency(instantiatedServiceName).install();
        // CREATE stage
        final String createName = "create"; // TODO - fetch from configuration
        final String destroyName = "destroy";
        final Method create = getInstanceMethod(index, createName);
        final Method destroy = getInstanceMethod(index, destroyName);
        final McLifecycleService<T> createService = new McLifecycleService<T>(create, destroy);
        final ServiceName createServiceName = beanServiceName.append(BeanState.CREATE.name());
        final ServiceBuilder<T> createServiceBuilder = serviceTarget.addService(createServiceName, createService);
        // TODO - add declared dependencies and injections for this stage -here-
        createServiceBuilder.addDependency(configuredServiceName, clazz, createService.getValueInjector()).install();
        // START stage
        final String startName = "start"; // TODO - fetch from configuration
        final String stopName = "stop";
        final Method start = getInstanceMethod(index, startName);
        final Method stop = getInstanceMethod(index, stopName);
        final McLifecycleService<T> startService = new McLifecycleService<T>(start, stop);
        final ServiceName startServiceName = beanServiceName.append(BeanState.START.name());
        final ServiceBuilder<T> startServiceBuilder = serviceTarget.addService(startServiceName, startService);
        // TODO - add declared dependencies and injections for this stage -here-
        startServiceBuilder.addDependency(createServiceName, clazz, startService.getValueInjector()).install();
        // INSTALLED stage
        // TODO - install actions go -here-
        final ServiceName installedServiceName = beanServiceName.append(BeanState.INSTALLED.name());
        final ServiceBuilder<T> installedServiceBuilder = serviceTarget.addService(installedServiceName, new ValueService<T>(startService));
        // TODO - add declared dependencies and injections for this stage -here-
        installedServiceBuilder.addDependency(startServiceName).install();
    }

    private static Method getInstanceMethod(ClassReflectionIndex<?> index, String name, Class<?>... params) {
        final Collection<Method> methods = index.getMethods(name, params);
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            // take first match
            return method;
        }
        return null;
    }
}
