/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package com.redhat.gss.extension.requesthandler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;

import com.redhat.gss.extension.RedhatAccessPluginExtension;
import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.parsers.Solution;

import java.net.MalformedURLException;

public class GetSolutionRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {

    public static final String OPERATION_NAME = "get-solution";
    public static final GetSolutionRequestHandler INSTANCE = new GetSolutionRequestHandler();

    private static final SimpleAttributeDefinition SOLUTIONID = new SimpleAttributeDefinitionBuilder(
            "solution-id", ModelType.STRING).build();

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginExtension.getResourceDescriptionResolver())
            .setParameters(getParameters(SOLUTIONID))
            .setReplyType(ModelType.OBJECT)
            .setReplyParameters(
                    new SimpleAttributeDefinitionBuilder("id", ModelType.STRING)
                            .build(),
                    new SimpleAttributeDefinitionBuilder("title",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("issue",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("environment",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("resolution",
                            ModelType.STRING, true).build()).build();

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {

        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation)
                    throws OperationFailedException {
                API api = getAPI(context, operation);
                String solutionIdString = SOLUTIONID.resolveModelAttribute(
                        context, operation).asString();
                Solution solution = null;
                try {
                    solution = api.getSolutions().get(solutionIdString);
                } catch (Exception e) {
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                ModelNode response = context.getResult();
                if (solution.getId() != null) {
                    response.get("id").set(solution.getId());

                    if (solution.getTitle() != null) {
                        response.get("title").set(solution.getTitle());
                    }
                    if (solution.getIssue() != null) {
                        response.get("issue")
                                .set(solution.getIssue().getText());
                    }
                    if (solution.getEnvironment() != null) {
                        response.get("environment").set(
                                solution.getEnvironment().getText());
                    }
                    if (solution.getResolution() != null) {
                        response.get("resolution").set(
                                solution.getResolution().getText());
                    }

                }
                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
