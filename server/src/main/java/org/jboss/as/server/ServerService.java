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

package org.jboss.as.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.DeployerChainsService;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.ServiceLoaderProcessor;
import org.jboss.as.server.deployment.SubDeploymentProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndexProcessor;
import org.jboss.as.server.deployment.module.AdditionalModuleProcessor;
import org.jboss.as.server.deployment.module.DeploymentRootMountProcessor;
import org.jboss.as.server.deployment.module.DeploymentStructureDescriptorParser;
import org.jboss.as.server.deployment.module.ManifestAttachmentProcessor;
import org.jboss.as.server.deployment.module.ManifestClassPathProcessor;
import org.jboss.as.server.deployment.module.ManifestExtensionListProcessor;
import org.jboss.as.server.deployment.module.ManifestExtensionNameProcessor;
import org.jboss.as.server.deployment.module.ModuleClassPathProcessor;
import org.jboss.as.server.deployment.module.ModuleDependencyProcessor;
import org.jboss.as.server.deployment.module.ModuleExtensionListProcessor;
import org.jboss.as.server.deployment.module.ModuleExtensionNameProcessor;
import org.jboss.as.server.deployment.module.ModuleIdentifierProcessor;
import org.jboss.as.server.deployment.module.ModuleSpecProcessor;
import org.jboss.as.server.deployment.module.SubDeploymentDependencyProcessor;
import org.jboss.as.server.deployment.reflect.InstallReflectionIndexProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorDependencyProcessor;
import org.jboss.as.server.deployment.service.ServiceActivatorProcessor;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerService extends AbstractControllerService {

    /**
     * Construct a new instance.
     *
     * @param configurationPersister the configuration persister for this server
     */
    public ServerService(final ConfigurationPersister configurationPersister) {
        super(NewOperationContext.Type.SERVER, configurationPersister, ServerDescriptionProviders.ROOT_PROVIDER);
    }

    public void start(final StartContext context) throws StartException {
        super.start(context);
    }

    protected void boot(final StartContext context) throws ConfigurationPersistenceException {
        final EnumMap<Phase, Set<RegisteredProcessor>> deployers = new EnumMap<Phase, Set<RegisteredProcessor>>(Phase.class);
        for (Phase phase : Phase.values()) {
            deployers.put(phase, new TreeSet<RegisteredProcessor>());
        }
        final ThreadLocal<DeploymentProcessorTarget> local = AbstractDeploymentChainStep.PROCESSOR_TARGET_THREAD_LOCAL;
        local.set(new DeploymentProcessorTarget() {
            public void addDeploymentProcessor(final Phase phase, final int priority, final DeploymentUnitProcessor processor) {
                deployers.get(phase).add(new RegisteredProcessor(priority, processor));
            }
        });
        // Activate core processors for jar deployment
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_MOUNT, new DeploymentRootMountProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_MANIFEST, new ManifestAttachmentProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_ADDITIONAL_MANIFEST, new ManifestAttachmentProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_SUB_DEPLOYMENT, new SubDeploymentProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_MODULE_IDENTIFIERS, new ModuleIdentifierProcessor()));
        deployers.get(Phase.STRUCTURE).add(new RegisteredProcessor(Phase.STRUCTURE_ANNOTATION_INDEX, new AnnotationIndexProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_STRUCTURE_DESCRIPTOR, new DeploymentStructureDescriptorParser()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_COMPOSITE_ANNOTATION_INDEX, new CompositeIndexProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_ADDITIONAL_MODULES, new AdditionalModuleProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_CLASS_PATH, new ManifestClassPathProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_EXTENSION_LIST, new ManifestExtensionListProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_EXTENSION_NAME, new ManifestExtensionNameProcessor()));
        deployers.get(Phase.PARSE).add(new RegisteredProcessor(Phase.PARSE_SERVICE_LOADER_DEPLOYMENT, new ServiceLoaderProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_MODULE, new ModuleDependencyProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_SAR_MODULE, new ServiceActivatorDependencyProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_CLASS_PATH, new ModuleClassPathProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_EXTENSION_LIST, new ModuleExtensionListProcessor()));
        deployers.get(Phase.DEPENDENCIES).add(new RegisteredProcessor(Phase.DEPENDENCIES_SUB_DEPLOYMENTS, new SubDeploymentDependencyProcessor()));
        deployers.get(Phase.CONFIGURE_MODULE).add(new RegisteredProcessor(Phase.CONFIGURE_MODULE_SPEC, new ModuleSpecProcessor()));
        deployers.get(Phase.POST_MODULE).add(new RegisteredProcessor(Phase.POST_MODULE_INSTALL_EXTENSION, new ModuleExtensionNameProcessor()));
        deployers.get(Phase.INSTALL).add(new RegisteredProcessor(Phase.INSTALL_REFLECTION_INDEX, new InstallReflectionIndexProcessor()));
        deployers.get(Phase.INSTALL).add(new RegisteredProcessor(Phase.INSTALL_SERVICE_ACTIVATOR, new ServiceActivatorProcessor()));

        try {
            super.boot(context);
        } finally {
            local.set(null);
        }

        final EnumMap<Phase, List<DeploymentUnitProcessor>> finalDeployers = new EnumMap<Phase, List<DeploymentUnitProcessor>>(Phase.class);
        final List<DeploymentUnitProcessor> processorList = new ArrayList<DeploymentUnitProcessor>(256);
        for (Phase phase : Phase.values()) {
            processorList.clear();
            final Set<RegisteredProcessor> processorSet = deployers.get(phase);
            for (RegisteredProcessor processor : processorSet) {
                processorList.add(processor.getProcessor());
            }
            finalDeployers.put(phase, Arrays.asList(processorList.toArray(new DeploymentUnitProcessor[processorList.size()])));
        }
        DeployerChainsService.addService(context.getChildTarget(), finalDeployers);
    }

    public void stop(final StopContext context) {
        super.stop(context);
    }

    static final class RegisteredProcessor implements Comparable<RegisteredProcessor> {
        private final int priority;
        private final DeploymentUnitProcessor processor;

        RegisteredProcessor(final int priority, final DeploymentUnitProcessor processor) {
            this.priority = priority;
            this.processor = processor;
        }

        @Override
        public int compareTo(final RegisteredProcessor o) {
            final int rel = Integer.signum(priority - o.priority);
            return rel == 0 ? processor.getClass().getName().compareTo(o.getClass().getName()) : rel;
        }

        int getPriority() {
            return priority;
        }

        DeploymentUnitProcessor getProcessor() {
            return processor;
        }
    }
}
