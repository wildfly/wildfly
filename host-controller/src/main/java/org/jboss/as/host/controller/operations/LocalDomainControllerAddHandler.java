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


import java.util.List;
import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.controller.DomainModelImpl;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.host.controller.DomainModelProxy;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.LocalFileRepository;
import static org.jboss.as.host.controller.operations.DomainControllerAddUtil.installLocalDomainController;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @version $Revision: 1.1 $
 */
public class LocalDomainControllerAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = "write-local-domain-controller";

    public static ModelNode getAddDomainControllerOperation(ModelNode address) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        return op;
    }

    private final DomainModelProxy domainModelProxy;
    private final HostControllerEnvironment environment;

    public static LocalDomainControllerAddHandler getInstance(final DomainModelProxy domainModelProxy, final HostControllerEnvironment environment) {
        return new LocalDomainControllerAddHandler(domainModelProxy, environment);
    }

    /**
     * Create the ServerAddHandler
     */
    LocalDomainControllerAddHandler(DomainModelProxy domainModelProxy, HostControllerEnvironment environment) {
        this.domainModelProxy = domainModelProxy;
        this.environment = environment;
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(DOMAIN_CONTROLLER).get(LOCAL).setEmptyObject();
    }

    protected void performRuntime(NewOperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final DomainModelImpl domainModel = domainModelProxy.getDomainModel();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final FileRepository fileRepository = new LocalFileRepository(environment);
        newControllers.addAll(installLocalDomainController(environment, domainModel.getHostModel(), serviceTarget, false, fileRepository, domainModelProxy.getDomainModel(), verificationHandler));
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        // TODO - Return valid ModelDescription.
        return new ModelNode();
    }
}
