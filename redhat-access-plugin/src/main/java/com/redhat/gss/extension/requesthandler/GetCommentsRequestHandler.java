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
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;

import com.redhat.gss.extension.RedhatAccessPluginExtension;
import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.parsers.Comment;
import java.net.MalformedURLException;
import java.util.List;

public class GetCommentsRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {

    public static final Logger logger = Logger.getLogger(GetCommentsRequestHandler.class);
    public static final String OPERATION_NAME = "get-comments";
    public static final GetCommentsRequestHandler INSTANCE = new GetCommentsRequestHandler();

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginExtension.getResourceDescriptionResolver())
            .setParameters(getParameters(CASENUMBER))
            .setReplyType(ModelType.LIST)
            .setReplyParameters(
                    new SimpleAttributeDefinitionBuilder("author",
                            ModelType.STRING).build(),
                    new SimpleAttributeDefinitionBuilder("date",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("text",
                            ModelType.STRING, true).build()).build();

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {

        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation)
                    throws OperationFailedException {
                API api = null;
                try {
                    api = getAPI(context, operation);
                } catch (MalformedURLException e) {
                    logger.error(e);
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                String caseNumberString = CASENUMBER.resolveModelAttribute(
                        context, operation).asString();

                List<Comment> comments = null;
                try {
                    comments = api.getComments().list(caseNumberString, null,
                            null, null);
                } catch (Exception e) {
                    logger.error(e);
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                ModelNode response = context.getResult();
                int i = 0;
                for (Comment comment : comments) {
                    if (comment.getId() != null) {
                        ModelNode com = response.get(i);
                        if (comment.getCreatedBy() != null) {
                            com.get("author").set(comment.getCreatedBy());
                        }
                        if (comment.getCreatedDate() != null) {
                            com.get("date").set(
                                    comment.getCreatedDate().getTime()
                                            .toString());
                        }
                        if (comment.getText() != null) {
                            com.get("text").set(comment.getText());
                        }
                        i++;

                    }
                }

                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
