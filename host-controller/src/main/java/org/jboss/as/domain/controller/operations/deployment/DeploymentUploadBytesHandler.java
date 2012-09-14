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
package org.jboss.as.domain.controller.operations.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the upload-deployment-bytes operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentUploadBytesHandler
    extends AbstractDeploymentUploadHandler {

    public static final String OPERATION_NAME = UPLOAD_DEPLOYMENT_BYTES;


    /**
     * Constructor
     *
     * @param repository the master content repository. If {@code null} this handler will function as a slave handler would.
     */
    private DeploymentUploadBytesHandler(final ContentRepository repository) {
        super(repository, DeploymentAttributes.BYTES_NOT_NULL);
    }

    public static void registerMaster(final ManagementResourceRegistration registration, final ContentRepository repository) {
        new DeploymentUploadBytesHandler(repository).register(registration);
    }

    public static void registerSlave(final ManagementResourceRegistration registration) {
        new DeploymentUploadBytesHandler(null).register(registration);
    }

    private void register(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(DeploymentAttributes.DOMAIN_UPLOAD_BYTES_DEFINITION, this);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream getContentInputStream(OperationContext operationContext, ModelNode operation) throws OperationFailedException {
        byte[] bytes = operation.get(BYTES).asBytes();
        return new ByteArrayInputStream(bytes);
    }

}
