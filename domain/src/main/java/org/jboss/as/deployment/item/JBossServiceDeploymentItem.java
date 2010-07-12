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

package org.jboss.as.deployment.item;

import org.jboss.as.deployment.descriptor.JBossServiceConfig;
import org.jboss.as.deployment.descriptor.JBossServiceConstructorConfig;
import org.jboss.as.deployment.descriptor.JBossServiceDependencyConfig;
import org.jboss.modules.Module;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchInjectionBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.ConstructedValue;
import org.jboss.msc.value.LookupClassValue;
import org.jboss.msc.value.LookupConstructorValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * @author John E. Bailey
 */
public class JBossServiceDeploymentItem implements DeploymentItem {
    private final JBossServiceConfig serviceConfig;

    public JBossServiceDeploymentItem(JBossServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public void install(DeploymentItemContext context) {
        final Module module = context.getModule();
        final ClassLoader classLoader = module.getClassLoader();
        final BatchBuilder batchBuilder = context.getBatchBuilder();

        final String codeName = serviceConfig.getCode();
        final LookupClassValue classValue = new LookupClassValue(codeName, Values.immediateValue(classLoader));

        final List<Value<?>> constructorArguments = new ArrayList<Value<?>>();
        final List<Value<Class<?>>> constructorSignature = new ArrayList<Value<Class<?>>>();

        final JBossServiceConstructorConfig constructorConfig = serviceConfig.getConstructorConfig();
        if(constructorConfig != null) {
            final JBossServiceConstructorConfig.Argument[] arguments = constructorConfig.getArguments();
            for(JBossServiceConstructorConfig.Argument argument : arguments) {
                final Object value = null; // TODO: Do something with argument.getValue()
                constructorArguments.add(Values.immediateValue(value));
                constructorSignature.add(new LookupClassValue(argument.getType(), Values.immediateValue(classLoader)));
            }
        }

        final LookupConstructorValue constructorValue = new LookupConstructorValue(classValue, constructorSignature);
        final ConstructedValue constructedValue = new ConstructedValue(constructorValue, constructorArguments);
        final JBossService<?> jBossService = new JBossService<Object>(constructedValue);

        final String serviceName = serviceConfig.getName();
        final BatchServiceBuilder<?> serviceBuilder = batchBuilder.addService(ServiceName.of(serviceName), jBossService);
        final JBossServiceDependencyConfig[] dependencyConfigs = serviceConfig.getDependencyConfigs();
        for(JBossServiceDependencyConfig dependencyConfig : dependencyConfigs) {
            final BatchInjectionBuilder injectionBuilder = serviceBuilder.addDependency(ServiceName.of(dependencyConfig.getDependencyName()));
            final String optionalAttributeName = dependencyConfig.getOptionalAttributeName();
            if(optionalAttributeName != null) {
                injectionBuilder.toProperty(optionalAttributeName);
            }
        }
        // TODO: What should we do with the MBEAN attributes
    }
}
