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


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;

import java.util.Locale;

import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.as.host.controller.HostControllerConfigurationPersister;
import org.jboss.as.host.controller.descriptions.HostRootDescription;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RemoteDomainControllerAddHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "write-remote-domain-controller";

    public static final SimpleAttributeDefinition PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PORT, ModelType.INT, false)
            .setAllowExpression(true).setValidator(new IntRangeValidator(1, 65535, false, true)).setFlags(AttributeAccess.Flag.RESTART_JVM).build();

    public static final SimpleAttributeDefinition HOST = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HOST, ModelType.STRING, false)
            .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true)).setFlags(AttributeAccess.Flag.RESTART_JVM).build();

    private final ManagementResourceRegistration rootRegistration;
    private final HostControllerConfigurationPersister overallConfigPersister;
    private final ContentRepository contentRepository;
    private final HostFileRepository fileRepository;
    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final ExtensionRegistry extensionRegistry;
    private final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry;

    public RemoteDomainControllerAddHandler(final ManagementResourceRegistration rootRegistration,
                                               final LocalHostControllerInfoImpl hostControllerInfo,
                                               final HostControllerConfigurationPersister overallConfigPersister,
                                               final ContentRepository contentRepository,
                                               final HostFileRepository fileRepository,
                                               final ExtensionRegistry extensionRegistry,
                                               final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry) {
        this.rootRegistration = rootRegistration;
        this.overallConfigPersister = overallConfigPersister;
        this.contentRepository = contentRepository;
        this.fileRepository = fileRepository;
        this.hostControllerInfo = hostControllerInfo;
        this.extensionRegistry = extensionRegistry;
        this.ignoredDomainResourceRegistry = ignoredDomainResourceRegistry;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();
        ModelNode dc = model.get(DOMAIN_CONTROLLER);
        ModelNode remoteDC = dc.get(REMOTE);

        PORT.validateAndSet(operation, remoteDC);
        HOST.validateAndSet(operation, remoteDC);
        if (operation.has(SECURITY_REALM)) {
            ModelNode securityRealm = operation.require(SECURITY_REALM);
            dc.get(REMOTE, SECURITY_REALM).set(securityRealm);
            hostControllerInfo.setRemoteDomainControllerSecurityRealm(securityRealm.resolve().asString());
        }

        if (dc.has(LOCAL)) {
            dc.remove(LOCAL);
        }

        initializeDomain(context, remoteDC);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    protected void initializeDomain(OperationContext context, ModelNode remoteDC) throws OperationFailedException {
        hostControllerInfo.setMasterDomainController(false);
        hostControllerInfo.setRemoteDomainControllerHost(HOST.resolveModelAttribute(context, remoteDC).asString());
        hostControllerInfo.setRemoteDomainControllerPort(PORT.resolveModelAttribute(context, remoteDC).asInt());

        overallConfigPersister.initializeDomainConfigurationPersister(true);

        DomainModelUtil.initializeSlaveDomainRegistry(rootRegistration, overallConfigPersister.getDomainPersister(),
                contentRepository, fileRepository, hostControllerInfo, extensionRegistry, ignoredDomainResourceRegistry);
    }

    //Done by DomainModelControllerService
//    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
//        final ModelNode hostModel = context.readModel(PathAddress.EMPTY_ADDRESS);
//        final ServiceTarget serviceTarget = context.getServiceTarget();
//        newControllers.add(installRemoteDomainControllerConnection(hostModel, serviceTarget, fileRepository));
//        newControllers.addAll(installLocalDomainController(hostModel, serviceTarget, true, verificationHandler));
//    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return HostRootDescription.getRemoteDomainControllerAdd(locale);
    }
}
