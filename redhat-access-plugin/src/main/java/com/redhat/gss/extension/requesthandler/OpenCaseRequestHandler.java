/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import com.redhat.gss.extension.RedhatAccessPluginExtension;
import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.parsers.Case;
import java.net.MalformedURLException;

public class OpenCaseRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {

    public static final String OPERATION_NAME = "open-case";
    public static final OpenCaseRequestHandler INSTANCE = new OpenCaseRequestHandler();

    private static final SimpleAttributeDefinition SUMMARY = new SimpleAttributeDefinitionBuilder(
            "Summary", ModelType.STRING).setAllowExpression(true)
            .setXmlName("Summary")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    private static final SimpleAttributeDefinition DESCRIPTION = new SimpleAttributeDefinitionBuilder(
            "Description", ModelType.STRING).setAllowExpression(true)
            .setXmlName("Description")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    private static final SimpleAttributeDefinition SEVERITY = new SimpleAttributeDefinitionBuilder(
            "Severity", ModelType.STRING, true).setAllowExpression(true)
            .setXmlName("Severity")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    private static final SimpleAttributeDefinition PRODUCT = new SimpleAttributeDefinitionBuilder(
            "Product", ModelType.STRING).setAllowExpression(true)
            .setXmlName("Product")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
    private static final SimpleAttributeDefinition VERSION = new SimpleAttributeDefinitionBuilder(
            "Version", ModelType.STRING).setAllowExpression(true)
            .setXmlName("Version")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginExtension.getResourceDescriptionResolver())
            .setParameters(
                    getParameters(SUMMARY, DESCRIPTION, SEVERITY, PRODUCT,
                            VERSION)).build();

    @Override
    public void execute(OperationContext context, ModelNode operation)
            throws OperationFailedException {
        // In MODEL stage, just validate the request. Unnecessary if the request
        // has no parameters
        validator.validate(operation);
        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation)
                    throws OperationFailedException {
                API api = null;
                try {
                    api = getAPI(context, operation);
                } catch (MalformedURLException e) {
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                Case cas = new Case();
                cas.setSummary(SUMMARY
                        .resolveModelAttribute(context, operation).asString());
                cas.setDescription(DESCRIPTION.resolveModelAttribute(context,
                        operation).asString());
                if (SEVERITY.resolveModelAttribute(context, operation)
                        .isDefined()) {
                    cas.setSeverity(SEVERITY.resolveModelAttribute(context,
                            operation).asString());
                }
                cas.setProduct(PRODUCT
                        .resolveModelAttribute(context, operation).asString());
                cas.setVersion(VERSION
                        .resolveModelAttribute(context, operation).asString());
                try {
                    cas = api.getCases().add(cas);
                } catch (Exception e) {
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                ModelNode response = context.getResult();
                response.get("CaseNumber").set(cas.getCaseNumber());
                response.get("Summary").set(cas.getSummary());
                response.get("Description").set(cas.getDescription());
                response.get("Product").set(cas.getProduct());
                response.get("Version").set(cas.getVersion());
                if (cas.getSeverity() != null) {
                    response.get("Severity").set(cas.getSeverity());
                }

                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
