/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller.operations;


import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;

import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainContentRepository;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.as.domain.controller.UnregisteredHostChannelRegistry;
import org.jboss.as.host.controller.HostControllerConfigurationPersister;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @version $Revision: 1.1 $
 */
public class LocalDomainControllerAddHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "write-local-domain-controller";

    private final ManagementResourceRegistration rootRegistration;
    private final HostControllerEnvironment environment;
    private final HostControllerConfigurationPersister overallConfigPersister;
    private final FileRepository fileRepository;
    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final DomainController domainController;
    private final UnregisteredHostChannelRegistry registry;

    public static LocalDomainControllerAddHandler getInstance(final ManagementResourceRegistration rootRegistration,
                                                                 final LocalHostControllerInfoImpl hostControllerInfo,
                                                                 final HostControllerEnvironment environment,
                                                                 final HostControllerConfigurationPersister overallConfigPersister,
                                                                 final FileRepository fileRepository,
                                                                 final DomainController domainController,
                                                                 final UnregisteredHostChannelRegistry registry) {
        return new LocalDomainControllerAddHandler(rootRegistration, hostControllerInfo, environment, overallConfigPersister, fileRepository, domainController, registry);
    }

    /**
     * Create the ServerAddHandler
     */
    protected LocalDomainControllerAddHandler(final ManagementResourceRegistration rootRegistration,
                                    final LocalHostControllerInfoImpl hostControllerInfo,
                                    final HostControllerEnvironment environment,
                                    final HostControllerConfigurationPersister overallConfigPersister,
                                    final FileRepository fileRepository,
                                    final DomainController domainController,
                                    final UnregisteredHostChannelRegistry registry) {
        this.environment = environment;
        this.rootRegistration = rootRegistration;
        this.overallConfigPersister = overallConfigPersister;
        this.fileRepository = fileRepository;
        this.hostControllerInfo = hostControllerInfo;
        this.domainController = domainController;
        this.registry = registry;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();

        ModelNode dc = model.get(DOMAIN_CONTROLLER);
        dc.get(LOCAL).setEmptyObject();

        if (dc.has(REMOTE)) {
            dc.remove(REMOTE);
        }

        initializeDomain();

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    protected void initializeDomain() {
        hostControllerInfo.setMasterDomainController(true);
        overallConfigPersister.initializeDomainConfigurationPersister(false);

        ContentRepository contentRepo = new DomainContentRepository(environment.getDomainDeploymentDir());
        hostControllerInfo.setContentRepository(contentRepo);

        DomainModelUtil.initializeMasterDomainRegistry(rootRegistration, overallConfigPersister.getDomainPersister(),
                contentRepo, fileRepository, domainController, registry);
    }


    //Done by DomainModelControllerService
//    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
//                                  ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
//        final ModelNode hostModel = context.readModel(PathAddress.EMPTY_ADDRESS);
//        final ServiceTarget serviceTarget = context.getServiceTarget();
//        newControllers.addAll(installLocalDomainController(hostModel, serviceTarget, false, verificationHandler));
//    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return HostRootDescription.getLocalDomainControllerAdd(locale);
    }
}
