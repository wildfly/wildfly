/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
