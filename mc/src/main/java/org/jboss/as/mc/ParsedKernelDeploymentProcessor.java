/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
import java.util.Collections;
import java.util.List;

import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.mc.descriptor.BeanMetaDataConfig;
import org.jboss.as.mc.descriptor.KernelDeploymentXmlDescriptor;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ConstructedValue;
import org.jboss.msc.value.LookupClassValue;
import org.jboss.msc.value.LookupConstructorValue;
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
     * Process a deployment for KernelDeployment confguration.
     * Will install a {@code MC bean} for each configured bean.
     *
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final KernelDeploymentXmlDescriptor kdXmlDescriptor = context.getAttachment(KernelDeploymentXmlDescriptor.ATTACHMENT_KEY);
        if(kdXmlDescriptor == null)
            return;

        final Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if(module == null)
            throw new DeploymentUnitProcessingException("Failed to get module attachment for deployment: " + context.getName());

        final ClassLoader classLoader = module.getClassLoader();
        final Value<ClassLoader> classLoaderValue = Values.immediateValue(classLoader);

        final List<BeanMetaDataConfig> beanConfigs = kdXmlDescriptor.getBeans();
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        for(final BeanMetaDataConfig beanConfig : beanConfigs) {
            addBean(batchBuilder, beanConfig, classLoaderValue);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void addBean(BatchBuilder batchBuilder, BeanMetaDataConfig beanConfig, Value<ClassLoader> classLoaderValue) {
        final String className = beanConfig.getBeanClass();
        final Value<Class<?>> classValue = cached(new LookupClassValue(className, classLoaderValue));
        final List<? extends Value<Class<?>>> types = Collections.emptyList();
        final Value<Constructor> constructorValue = cached(new LookupConstructorValue(classValue, types));
        final List<? extends Value<?>> args = Collections.emptyList();
        final Value<Object> constructedValue = cached(new ConstructedValue(constructorValue, args));
        batchBuilder.addService(ServiceName.of(beanConfig.getName()), new AbstractService<Object>() {
            @Override
            public Object getValue() throws IllegalStateException {
                return constructedValue.getValue();
            }
        });
        // TODO -- missing a bunch of stuff
    }

    private static <T> Value<T> cached(final Value<T> value) {
        return Values.cached(value);
    }
}