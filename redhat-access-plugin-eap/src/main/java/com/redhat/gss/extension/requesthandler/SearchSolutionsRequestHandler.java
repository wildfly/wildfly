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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceRegistry;

import com.redhat.gss.extension.RedhatAccessPluginEapDescriptions;
import com.redhat.gss.extension.RedhatAccessPluginEapExtension;
import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.parsers.Solution;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SearchSolutionsRequestHandler extends BaseRequestHandler implements
		OperationStepHandler, DescriptionProvider {

	public static final String OPERATION_NAME = "search-solutions";
	public static final SearchSolutionsRequestHandler INSTANCE = new SearchSolutionsRequestHandler();

	public static final SimpleAttributeDefinition searchString = new SimpleAttributeDefinitionBuilder(
			"searchString", ModelType.STRING).setAllowExpression(true)
			.setXmlName("searchString")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

	public SearchSolutionsRequestHandler() {
		super(PathElement.pathElement(OPERATION_NAME), RedhatAccessPluginEapExtension
				.getResourceDescriptionResolver(OPERATION_NAME), INSTANCE,
				INSTANCE, OPERATION_NAME, searchString);
	}

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
				String[] searchStr = { searchString.resolveModelAttribute(
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
							solutionNode.get("ViewUri").set(solution.getViewUri());
						}
					}
				}
				context.completeStep();
			}
		}, OperationContext.Stage.RUNTIME);

		context.completeStep();
	}

	@Override
	public ModelNode getModelDescription(Locale locale) {
		return RedhatAccessPluginEapDescriptions.getRedhatAccessPluginEapRequestDescription(locale);
	}
}
