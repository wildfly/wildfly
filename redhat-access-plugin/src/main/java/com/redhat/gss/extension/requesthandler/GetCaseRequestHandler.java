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
import com.redhat.gss.redhat_support_lib.parsers.Case;
import java.net.MalformedURLException;

public class GetCaseRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {
    public static final Logger logger = Logger.getLogger(GetCaseRequestHandler.class);
    public static final String OPERATION_NAME = "get-case";
    public static final GetCaseRequestHandler INSTANCE = new GetCaseRequestHandler();;

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginExtension.getResourceDescriptionResolver())
            .setParameters(getParameters(CASENUMBER))
            .setReplyType(ModelType.OBJECT)
            .setReplyParameters(
                    new SimpleAttributeDefinitionBuilder("case-number",
                            ModelType.STRING).build(),
                    new SimpleAttributeDefinitionBuilder("summary",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("case-type",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("severity",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("status",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("alternate-id",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("product",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("owner",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("opened",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("last-updated",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("account-number",
                            ModelType.STRING, true).build(),
                    new SimpleAttributeDefinitionBuilder("description",
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
                Case cas = null;
                try {
                    cas = api.getCases().get(caseNumberString);
                } catch (Exception e) {
                    logger.error(e);
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                ModelNode response = context.getResult();
                if (cas.getCaseNumber() != null) {
                    response.get("case-number").set(cas.getCaseNumber());
                    if (cas.getSummary() != null) {
                        response.get("summary").set(cas.getSummary());
                    }
                    if (cas.getType() != null) {
                        response.get("case-type").set(cas.getType());
                    }
                    if (cas.getSeverity() != null) {
                        response.get("severity").set(cas.getSeverity());
                    }
                    if (cas.getStatus() != null) {
                        response.get("status").set(cas.getStatus());
                    }
                    if (cas.getAlternateId() != null) {
                        response.get("alternate-id").set(cas.getAlternateId());
                    }
                    if (cas.getProduct() != null) {
                        response.get("product").set(cas.getProduct());
                    }
                    if (cas.getOwner() != null) {
                        response.get("owner").set(cas.getOwner());
                    }
                    if (cas.getCreatedDate() != null) {
                        response.get("opened").set(
                                cas.getCreatedDate().getTime().toString());
                    }
                    if (cas.getLastModifiedDate() != null) {
                        response.get("last-updated").set(
                                cas.getLastModifiedDate().getTime().toString());
                    }
                    if (cas.getAccountNumber() != null) {
                        response.get("account-number").set(
                                cas.getAccountNumber());
                    }
                    if (cas.getDescription() != null) {
                        response.get("description").set(cas.getDescription());
                    }
                }

                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
