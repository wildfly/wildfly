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
import com.redhat.gss.redhat_support_lib.parsers.Comment;
import java.net.MalformedURLException;

public class AddCommentRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {
    public static final Logger logger = Logger.getLogger(AddCommentRequestHandler.class);
    public static final String OPERATION_NAME = "add-comment";
    public static final AddCommentRequestHandler INSTANCE = new AddCommentRequestHandler();

    private static final SimpleAttributeDefinition COMMENTTEXT = new SimpleAttributeDefinitionBuilder(
            "comment-text", ModelType.STRING).build();

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginExtension.getResourceDescriptionResolver())
            .setParameters(getParameters(CASENUMBER, COMMENTTEXT)).setReplyType(ModelType.OBJECT)
            .setReplyParameters(
                    new SimpleAttributeDefinitionBuilder("author", ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("date", ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("text", ModelType.STRING, true).build()
                )
                .build();

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
                String commentTextString = COMMENTTEXT.resolveModelAttribute(
                        context, operation).asString();

                Comment comment = new Comment();
                comment.setCaseNumber(caseNumberString);
                comment.setText(commentTextString);
                try {
                    comment = api.getComments().add(comment);
                } catch (Exception e) {
                    logger.error(e);
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                ModelNode response = context.getResult();

                if (comment.getCreatedBy() != null) {
                    response.get("author").set(comment.getCreatedBy());
                }
                if (comment.getCreatedDate() != null) {
                    response.get("date").set(
                            comment.getCreatedDate().getTime().toString());
                }
                if (comment.getText() != null) {
                    response.get("text").set(comment.getText());
                }

                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
