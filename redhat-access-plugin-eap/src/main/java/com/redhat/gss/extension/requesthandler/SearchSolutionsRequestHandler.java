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
import com.redhat.gss.extension.RedhatAccessPluginEapExtension;
import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.parsers.Solution;

import java.net.MalformedURLException;
import java.util.List;

public class SearchSolutionsRequestHandler extends BaseRequestHandler implements
        OperationStepHandler {

    public static final String OPERATION_NAME = "search-solutions";
    public static final SearchSolutionsRequestHandler INSTANCE = new SearchSolutionsRequestHandler();

    public static final SimpleAttributeDefinition SEARCHSTRING = new SimpleAttributeDefinitionBuilder(
            "searchString", ModelType.STRING).setAllowExpression(true)
            .setXmlName("searchString")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    public static SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginEapExtension
                    .getResourceDescriptionResolver())
            .setParameters(getParameters(SEARCHSTRING)).build();

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
                String[] searchStr = { SEARCHSTRING.resolveModelAttribute(
                        context, operation).asString() };
                List<Solution> solutions = null;
                try {
                    solutions = api.getSolutions().list(searchStr, null);
                } catch (Exception e) {
                    throw new OperationFailedException(e.getLocalizedMessage(),
                            e);
                }
                ModelNode response = context.getResult();
                int i = 0;
                for (Solution solution : solutions) {
                    if (solution.getId() != null) {
                        ModelNode solutionNode = response.get(i);

                        solutionNode.get("ID").set(solution.getId());

                        if (solution.getTitle() != null) {
                            solutionNode.get("Title").set(solution.getTitle());
                        }
                        i++;
                        if (solution.getViewUri() != null) {
                            solutionNode.get("ViewUri").set(
                                    solution.getViewUri());
                        }
                    }
                }
                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
