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

import static org.jboss.as.domain.controller.DomainControllerLogger.DEPLOYMENT_LOGGER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Base class for operation handlers that can handle the upload of deployment content.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractDeploymentUploadHandler implements OperationStepHandler {

    private static final String[] EMPTY = new String[0];
    private final ContentRepository contentRepository;

    protected AbstractDeploymentUploadHandler(final ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (contentRepository != null) {
            try {
                InputStream is = getContentInputStream(context, operation);
                try {
                    byte[] hash = contentRepository.addContent(is);
                    context.getResult().set(hash);
                }
                finally {
                    safeClose(is);
                }
            }
            catch (IOException e) {
                throw new OperationFailedException(new ModelNode().set(e.toString()));
            }
        }
        // else this is a slave domain controller and we should ignore this operation

        context.completeStep();
    }

    protected abstract InputStream getContentInputStream(OperationContext context, ModelNode operation) throws OperationFailedException;

    private static void safeClose(InputStream is) {
        if (is != null) {
            try {
                is.close();
            }
            catch (Exception e) {
                DEPLOYMENT_LOGGER.caughtExceptionClosingInputStream(e);
            }
        }
    }
}
