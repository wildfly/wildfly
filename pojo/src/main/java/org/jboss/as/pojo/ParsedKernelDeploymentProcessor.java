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

package org.jboss.as.pojo;

import org.jboss.as.pojo.descriptor.BeanMetaDataConfig;
import org.jboss.as.pojo.descriptor.ConfigVisitor;
import org.jboss.as.pojo.descriptor.DefaultConfigVisitor;
import org.jboss.as.pojo.descriptor.KernelDeploymentXmlDescriptor;
import org.jboss.as.pojo.logging.PojoLogger;
import org.jboss.as.pojo.service.DescribedPojoPhase;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

/**
 * DeploymentUnit processor responsible for taking KernelDeploymentXmlDescriptor
 * configuration and creating the corresponding services.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ParsedKernelDeploymentProcessor implements DeploymentUnitProcessor {
    /**
     * Process a deployment for KernelDeployment configuration.
     * Will install a {@code POJO} for each configured bean.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final List<KernelDeploymentXmlDescriptor> kdXmlDescriptors = unit.getAttachment(KernelDeploymentXmlDescriptor.ATTACHMENT_KEY);
        if (kdXmlDescriptors == null || kdXmlDescriptors.isEmpty())
            return;

        final Module module = unit.getAttachment(Attachments.MODULE);
        if (module == null)
            throw PojoLogger.ROOT_LOGGER.noModuleFound(unit);

        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        final DeploymentReflectionIndex index = unit.getAttachment(Attachments.REFLECTION_INDEX);
        if (index == null)
            throw PojoLogger.ROOT_LOGGER.missingReflectionIndex(unit);

        for (KernelDeploymentXmlDescriptor kdXmlDescriptor : kdXmlDescriptors) {
            final List<BeanMetaDataConfig> beanConfigs = kdXmlDescriptor.getBeans();
            for (final BeanMetaDataConfig beanConfig : beanConfigs) {
                describeBean(module, serviceTarget, index, beanConfig);
            }
            // TODO -- KD::classloader, KD::aliases
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    protected void describeBean(final Module module, final ServiceTarget serviceTarget, DeploymentReflectionIndex deploymentIndex, BeanMetaDataConfig beanConfig) {
        final BeanState state = BeanState.NOT_INSTALLED;
        final ServiceName describedServiceName = BeanMetaDataConfig.toBeanName(beanConfig.getName(), state.next());
        final DescribedPojoPhase describedService = new DescribedPojoPhase(deploymentIndex, beanConfig);
        final ServiceBuilder describedServiceBuilder = serviceTarget.addService(describedServiceName, describedService);
        describedService.registerAliases(describedServiceBuilder);
        final ConfigVisitor visitor = new DefaultConfigVisitor(describedServiceBuilder, state, module, deploymentIndex);
        beanConfig.visit(visitor);
        describedServiceBuilder.setInitialMode(beanConfig.getMode().getMode());
        describedServiceBuilder.install();
    }
}
