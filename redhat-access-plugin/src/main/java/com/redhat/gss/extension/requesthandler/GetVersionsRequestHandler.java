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
import java.net.MalformedURLException;
import java.util.List;

public class GetVersionsRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {
    public static final Logger logger = Logger.getLogger(GetVersionsRequestHandler.class);
    public static final String OPERATION_NAME = "get-versions";
    public static final GetVersionsRequestHandler INSTANCE = new GetVersionsRequestHandler();

    private static final SimpleAttributeDefinition PRODUCT = new SimpleAttributeDefinitionBuilder(
            "product", ModelType.STRING).build();

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginExtension.getResourceDescriptionResolver())
            .setParameters(getParameters(PRODUCT))
            .setReplyType(ModelType.LIST)
            .setReplyValueType(ModelType.STRING).build();

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
                String productString = PRODUCT.resolveModelAttribute(context,
                        operation).asString();
                List<String> versions = null;
                try {
                    versions = api.getProducts().getVersions(productString);
                } catch (Exception e) {
                    logger.error(e);
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                ModelNode response = context.getResult();
                int i = 0;
                for (String version : versions) {
                    if (version != null) {
                        response.get(i).set(version);
                        i++;
                    }
                }

                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
