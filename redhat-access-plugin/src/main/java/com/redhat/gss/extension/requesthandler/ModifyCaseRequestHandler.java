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
import com.redhat.gss.redhat_support_lib.parsers.Case;
import java.net.MalformedURLException;

public class ModifyCaseRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {
    public static final String OPERATION_NAME = "modify-case";
    public static final ModifyCaseRequestHandler INSTANCE = new ModifyCaseRequestHandler();

    private static final SimpleAttributeDefinition SUMMARY = new SimpleAttributeDefinitionBuilder(
            "summary", ModelType.STRING, true).build();
    private static final SimpleAttributeDefinition DESCRIPTION = new SimpleAttributeDefinitionBuilder(
            "description", ModelType.STRING, true).build();
    private static final SimpleAttributeDefinition SEVERITY = new SimpleAttributeDefinitionBuilder(
            "severity", ModelType.STRING, true).build();
    private static final SimpleAttributeDefinition PRODUCT = new SimpleAttributeDefinitionBuilder(
            "product", ModelType.STRING, true).build();
    private static final SimpleAttributeDefinition VERSION = new SimpleAttributeDefinitionBuilder(
            "version", ModelType.STRING, true).build();

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginExtension.getResourceDescriptionResolver())
            .setParameters(
                    getParameters(CASENUMBER, SUMMARY, DESCRIPTION, SEVERITY,
                            PRODUCT, VERSION))
            .setReplyType(ModelType.OBJECT)
            .setReplyParameters(
                    new SimpleAttributeDefinitionBuilder("case-number",
                            ModelType.STRING).build(),
                    new SimpleAttributeDefinitionBuilder("summary",
                            ModelType.STRING).build(),
                    new SimpleAttributeDefinitionBuilder("description",
                            ModelType.STRING).build(),
                    new SimpleAttributeDefinitionBuilder("product",
                            ModelType.STRING).build(),
                    new SimpleAttributeDefinitionBuilder("version",
                            ModelType.STRING).build(),
                    new SimpleAttributeDefinitionBuilder("severity",
                            ModelType.STRING, true).build()).build();

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {

        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation)
                    throws OperationFailedException {
                API api = getAPI(context, operation);

                Case cas = null;
                String caseNumString = CASENUMBER.resolveModelAttribute(
                        context, operation).asString();
                try {
                    cas = api.getCases().get(caseNumString);
                } catch (Exception e) {
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }

                if (SUMMARY.resolveModelAttribute(context, operation)
                        .isDefined()) {
                    cas.setSummary(SUMMARY.resolveModelAttribute(context,
                            operation).asString());
                }
                if (DESCRIPTION.resolveModelAttribute(context, operation)
                        .isDefined()) {
                    cas.setDescription(DESCRIPTION.resolveModelAttribute(
                            context, operation).asString());
                }
                if (SEVERITY.resolveModelAttribute(context, operation)
                        .isDefined()) {
                    cas.setSeverity(SEVERITY.resolveModelAttribute(context,
                            operation).asString());
                }
                if (PRODUCT.resolveModelAttribute(context, operation)
                        .isDefined()) {
                    cas.setProduct(PRODUCT.resolveModelAttribute(context,
                            operation).asString());
                }
                if (VERSION.resolveModelAttribute(context, operation)
                        .isDefined()) {
                    cas.setVersion(VERSION.resolveModelAttribute(context,
                            operation).asString());
                }

                try {
                    cas = api.getCases().update(cas);
                } catch (Exception e) {
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }

                ModelNode response = context.getResult();
                response.get("case-number").set(cas.getCaseNumber());
                response.get("summary").set(cas.getSummary());
                response.get("description").set(cas.getDescription());
                response.get("product").set(cas.getProduct());
                response.get("version").set(cas.getVersion());
                if (cas.getSeverity() != null) {
                    response.get("severity").set(cas.getSeverity());
                }

                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
