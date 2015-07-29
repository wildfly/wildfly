/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentDependencyProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentParsingProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentProcessor;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentSchema;
import org.wildfly.extension.clustering.singleton.deployment.SingletonDeploymentXMLReader;

/**
 * @author Paul Ferraro
 */
public class SingletonDeployerServiceHandler implements ResourceServiceHandler {

    /**
     * {@inheritDoc}
     */
    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {

        OperationStepHandler step = new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget target) {
                for (SingletonDeploymentSchema schema : SingletonDeploymentSchema.values()) {
                    target.addDeploymentProcessor(SingletonDeployerExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_SINGLETON_DEPLOYMENT, new JBossAllXmlParserRegisteringProcessor<>(schema.getRoot(), SingletonDeploymentDependencyProcessor.CONFIGURATION_KEY, new SingletonDeploymentXMLReader(schema)));
                }
                target.addDeploymentProcessor(SingletonDeployerExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_SINGLETON_DEPLOYMENT, new SingletonDeploymentParsingProcessor());
                target.addDeploymentProcessor(SingletonDeployerExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_SINGLETON_DEPLOYMENT, new SingletonDeploymentDependencyProcessor());
                target.addDeploymentProcessor(SingletonDeployerExtension.SUBSYSTEM_NAME, Phase.CONFIGURE_MODULE, Phase.CONFIGURE_SINGLETON_DEPLOYMENT, new SingletonDeploymentProcessor());
            }
        };
        context.addStep(step, OperationContext.Stage.RUNTIME);

        String defaultPolicy = SingletonDeployerResourceDefinition.Attribute.DEFAULT.getDefinition().resolveModelAttribute(context, model).asString();
        ServiceName serviceName = new DeploymentPolicyServiceNameProvider().getServiceName();
        ServiceName targetServiceName = new DeploymentPolicyServiceNameProvider(defaultPolicy).getServiceName();
        new AliasServiceBuilder<>(serviceName, targetServiceName, DeploymentPolicy.class).build(context.getServiceTarget()).install();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        context.removeService(new DeploymentPolicyServiceNameProvider().getServiceName());
    }
}
