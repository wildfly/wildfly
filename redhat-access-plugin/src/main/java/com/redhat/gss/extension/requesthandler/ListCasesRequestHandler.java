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
import com.redhat.gss.extension.RedhatAccessPluginExtension;
import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.parsers.Case;
import java.util.List;

public class ListCasesRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {
    public static final String OPERATION_NAME = "list-cases";
    public static final ListCasesRequestHandler INSTANCE = new ListCasesRequestHandler();

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginExtension.getResourceDescriptionResolver())
            .setParameters(getParameters())
            .setReplyType(ModelType.LIST)
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
                API api = getAPI(context, operation);

                List<Case> cases = null;
                try {
                    cases = api.getCases().list(null, false, true, null, null,
                            null, null, null, null);
                } catch (Exception e) {
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                ModelNode response = context.getResult();
                int i = 0;
                for (Case cas : cases) {
                    if (cas.getCaseNumber() != null) {
                        ModelNode caseNode = response.get(i);
                        caseNode.get("case-number").set(cas.getCaseNumber());
                        if (cas.getSummary() != null) {
                            caseNode.get("summary").set(cas.getSummary());
                        }
                        if (cas.getType() != null) {
                            caseNode.get("case-type").set(cas.getType());
                        }
                        if (cas.getSeverity() != null) {
                            caseNode.get("severity").set(cas.getSeverity());
                        }
                        if (cas.getStatus() != null) {
                            caseNode.get("status").set(cas.getStatus());
                        }
                        if (cas.getAlternateId() != null) {
                            caseNode.get("alternate-id").set(
                                    cas.getAlternateId());
                        }
                        if (cas.getProduct() != null) {
                            caseNode.get("product").set(cas.getProduct());
                        }
                        if (cas.getOwner() != null) {
                            caseNode.get("owner").set(cas.getOwner());
                        }
                        if (cas.getCreatedDate() != null) {
                            caseNode.get("opened").set(
                                    cas.getCreatedDate().getTime().toString());
                        }
                        if (cas.getLastModifiedDate() != null) {
                            caseNode.get("last-updated").set(
                                    cas.getLastModifiedDate().getTime()
                                            .toString());
                        }
                        if (cas.getAccountNumber() != null) {
                            caseNode.get("account-number").set(
                                    cas.getAccountNumber());
                        }
                        if (cas.getDescription() != null) {
                            caseNode.get("description").set(
                                    cas.getDescription());
                        }
                    }
                }

                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
