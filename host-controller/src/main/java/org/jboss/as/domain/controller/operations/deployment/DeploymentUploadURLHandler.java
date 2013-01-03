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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_URL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the upload-deployment-url operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentUploadURLHandler
extends AbstractDeploymentUploadHandler {

    public static final String OPERATION_NAME = UPLOAD_DEPLOYMENT_URL;

    /**
     * Constructor
     *
     * @param repository the master content repository. If {@code null} this handler will function as a slave handler would.
     */
    private DeploymentUploadURLHandler(final ContentRepository repository) {
        super(repository, DeploymentAttributes.URL_NOT_NULL);
    }

    public static void registerMaster(final ManagementResourceRegistration registration, final ContentRepository repository) {
        new DeploymentUploadURLHandler(repository).register(registration);
    }

    public static void registerSlave(final ManagementResourceRegistration registration) {
        new DeploymentUploadURLHandler(null).register(registration);
    }

    private void register(ManagementResourceRegistration registration) {
        registration.registerOperationHandler(DeploymentAttributes.DOMAIN_UPLOAD_URL_DEFINITION, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream getContentInputStream(OperationContext operationContext, ModelNode operation) throws OperationFailedException {

        String urlSpec = operation.get(URL).asString();
        try {
            URL url = new URL(urlSpec);
            return url.openStream();
        } catch (MalformedURLException e) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidUrl(urlSpec, e.toString())));
        } catch (IOException e) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.errorObtainingUrlStream(urlSpec, e.toString())));
        }
    }

}
