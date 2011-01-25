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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URL;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.server.controller.descriptions.DeploymentDescription;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the upload-deployment-url operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentUploadURLHandler
extends AbstractDeploymentUploadHandler
implements DescriptionProvider {

    public static final String OPERATION_NAME = "upload-deployment-url";

    private final ParametersValidator urlValidator = new ParametersValidator();

    public DeploymentUploadURLHandler(final DeploymentRepository repository) {
        super(repository);
        this.urlValidator.registerValidator(URL, new StringLengthValidator(1));
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getUploadDeploymentURLOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream getContentInputStream(ModelNode operation) {
        String msg = urlValidator.validate(operation);
        if (msg != null) {
            throw new IllegalStateException(msg);
        }
        String urlSpec = operation.get(URL).asString();
        try {
            URL url = new URL(urlSpec);
            return url.openStream();
        } catch (MalformedURLException e) {
            throw new RuntimeException(urlSpec + " is not a valid URL", e);
        } catch (IOException e) {
            throw new RuntimeException("Error obtaining input stream from URL " + urlSpec, e);
        }
    }

}
