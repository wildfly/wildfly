/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DeploymentGcHandler implements OperationStepHandler, DescriptionProvider {

    public static String OPERATION_NAME = "gc-deployments";

    private final ContentRepository contentRepository;

    public DeploymentGcHandler(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final List<byte[]> hashes = new ArrayList<byte[]>();
        for (Resource resource : context.getRootResource().getChildren(DEPLOYMENT)) {
            final ModelNode model = resource.getModel();
            if (model.hasDefined(CONTENT)) {
                for (ModelNode contentElement : model.get(CONTENT).asList()) {
                    if (contentElement.hasDefined(HASH)) {
                        final byte[] hash = contentElement.get(HASH).asBytes();
                        hashes.add(hash);
                    }
                }
            }
        }

        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                contentRepository.purgeContent(hashes);
                context.completeStep();
            }
        }, Stage.RUNTIME);
        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        throw new IllegalStateException("NYI");
    }


}
