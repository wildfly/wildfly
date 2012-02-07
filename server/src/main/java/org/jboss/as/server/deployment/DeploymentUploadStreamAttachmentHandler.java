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
package org.jboss.as.server.deployment;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.DeploymentDescription;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerMessages;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INPUT_STREAM_INDEX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UPLOAD_DEPLOYMENT_STREAM;

/**
* Handler for the upload-deployment-stream operation.
*
* @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
*/
public class DeploymentUploadStreamAttachmentHandler
    extends AbstractDeploymentUploadHandler
    implements DescriptionProvider {

    public static final String OPERATION_NAME = UPLOAD_DEPLOYMENT_STREAM;

    private final ParametersValidator streamValidator = new ParametersValidator();

    public DeploymentUploadStreamAttachmentHandler(final ContentRepository repository) {
        super(repository);
        this.streamValidator.registerValidator(INPUT_STREAM_INDEX, new IntRangeValidator(0));
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getUploadDeploymentStreamAttachmentOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream getContentInputStream(OperationContext operationContext, ModelNode operation) throws IOException, OperationFailedException {
        // Validate the operation
        streamValidator.validate(operation);
        // Get the attached stream
        final int streamIndex = operation.require(INPUT_STREAM_INDEX).asInt();
        final InputStream in = operationContext.getAttachmentStream(streamIndex);
        if (in == null) {
            throw ServerMessages.MESSAGES.nullStreamAttachment(streamIndex);
        }
        return in;
    }

}
