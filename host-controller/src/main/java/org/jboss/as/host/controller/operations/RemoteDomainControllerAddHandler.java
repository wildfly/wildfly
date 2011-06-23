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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;

import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.host.controller.HostControllerConfigurationPersister;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @version $Revision: 1.1 $
 */
public class RemoteDomainControllerAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = "write-remote-domain-controller";

    private final ParametersValidator parametersValidator = new ParametersValidator();

    private final ManagementResourceRegistration rootRegistration;
    private final HostControllerConfigurationPersister overallConfigPersister;
    private final FileRepository fileRepository;
    private final LocalHostControllerInfoImpl hostControllerInfo;

    public static RemoteDomainControllerAddHandler getInstance(final ManagementResourceRegistration rootRegistration,
                                                                 final LocalHostControllerInfoImpl hostControllerInfo,
                                                                 final HostControllerConfigurationPersister overallConfigPersister,
                                                                 final FileRepository fileRepository) {
        return new RemoteDomainControllerAddHandler(rootRegistration, hostControllerInfo, overallConfigPersister, fileRepository);
    }

    /**
     * Create the ServerAddHandler
     */
    RemoteDomainControllerAddHandler(final ManagementResourceRegistration rootRegistration,
                                     final LocalHostControllerInfoImpl hostControllerInfo,
                                     final HostControllerConfigurationPersister overallConfigPersister,
                                     final FileRepository fileRepository) {
        this.rootRegistration = rootRegistration;
        this.overallConfigPersister = overallConfigPersister;
        this.fileRepository = fileRepository;
        this.hostControllerInfo = hostControllerInfo;

        this.parametersValidator.registerValidator(PORT, new IntRangeValidator(1, 65535, false, true));
        this.parametersValidator.registerValidator(HOST, new StringLengthValidator(1, Integer.MAX_VALUE, false, true));
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        parametersValidator.validate(operation);
        ModelNode dc = model.get(DOMAIN_CONTROLLER);

        final ModelNode port = operation.require(PORT);
        final ModelNode host = operation.require(HOST);
        dc.get(REMOTE, PORT).set(port);
        dc.get(REMOTE, HOST).set(host);
        if (operation.has(SECURITY_REALM)) {
            ModelNode securityRealm = operation.require(SECURITY_REALM);
            dc.get(REMOTE, SECURITY_REALM).set(securityRealm);
            hostControllerInfo.setRemoteDomainControllerSecurityRealm(securityRealm.resolve().asString());
        }

        if (dc.has(LOCAL)) {
            dc.remove(LOCAL);
        }

        hostControllerInfo.setMasterDomainController(false);
        hostControllerInfo.setRemoteDomainControllerHost(host.resolve().asString());
        hostControllerInfo.setRemoteDomainControllerPort(port.resolve().asInt());

        overallConfigPersister.initializeDomainConfigurationPersister(true);

        DomainModelUtil.initializeSlaveDomainRegistry(rootRegistration, overallConfigPersister.getDomainPersister(), fileRepository);
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
        // TODO - Add the ModelDescription
        return new ModelNode();
    }
}
