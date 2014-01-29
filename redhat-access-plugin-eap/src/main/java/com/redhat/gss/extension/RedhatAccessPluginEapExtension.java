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

package com.redhat.gss.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import com.redhat.gss.extension.requesthandler.AddCommentRequestHandler;
import com.redhat.gss.extension.requesthandler.DiagnoseFileRequestHandler;
import com.redhat.gss.extension.requesthandler.DiagnoseStringRequestHandler;
import com.redhat.gss.extension.requesthandler.GetCaseRequestHandler;
import com.redhat.gss.extension.requesthandler.GetCommentsRequestHandler;
import com.redhat.gss.extension.requesthandler.GetSolutionRequestHandler;
import com.redhat.gss.extension.requesthandler.GetVersionsRequestHandler;
import com.redhat.gss.extension.requesthandler.ListCasesRequestHandler;
import com.redhat.gss.extension.requesthandler.ListProductsRequestHandler;
import com.redhat.gss.extension.requesthandler.ListSeveritiesRequestHandler;
import com.redhat.gss.extension.requesthandler.ModifyCaseRequestHandler;
import com.redhat.gss.extension.requesthandler.OpenCaseRequestHandler;
import com.redhat.gss.extension.requesthandler.SearchSolutionsRequestHandler;
import com.redhat.gss.extension.requesthandler.SymptomsFileRequestHandler;


public class RedhatAccessPluginEapExtension implements Extension {

	public static final String SUBSYSTEM_NAME = "redhat-access-plugin-eap";

	public static final String RESOURCE_NAME = RedhatAccessPluginEapExtension.class
			.getPackage().getName() + ".LocalDescriptions";
	
	public static StandardResourceDescriptionResolver getResourceDescriptionResolver(
			final String keyPrefix) {
		String prefix = RedhatAccessPluginEapExtension.SUBSYSTEM_NAME;
		return new StandardResourceDescriptionResolver(prefix, RedhatAccessPluginEapExtension.RESOURCE_NAME,
				SearchSolutionsRequestHandler.class.getClassLoader(), true, false);
	}

	public void initialize(ExtensionContext context) {
		final DescriptionProvider subsystemDescription = new DescriptionProvider() {
			public ModelNode getModelDescription(Locale locale) {
				return RedhatAccessPluginEapDescriptions.RedhatAccessPluginEapDescription(locale);
			}
		};

		SubsystemRegistration subsystemRegistration = context
				.registerSubsystem(SUBSYSTEM_NAME, 1, 0);

		ManagementResourceRegistration root = subsystemRegistration
				.registerSubsystemModel(subsystemDescription);
		root.registerOperationHandler(RedhatAccessPluginEapSubsystemAdd.OPERATION_NAME,
				RedhatAccessPluginEapSubsystemAdd.INSTANCE, RedhatAccessPluginEapSubsystemAdd.INSTANCE);
		root.registerOperationHandler(DESCRIBE,
				RedhatAccessPluginEapDescribeHandler.INSTANCE,
				RedhatAccessPluginEapDescribeHandler.INSTANCE, false,
				OperationEntry.EntryType.PRIVATE);
		root.registerOperationHandler(RedhatAccessPluginEapSubsystemRemove.OPERATION_NAME,
				RedhatAccessPluginEapSubsystemRemove.INSTANCE,
				RedhatAccessPluginEapSubsystemRemove.INSTANCE);
		if (context.isRuntimeOnlyRegistrationValid()) {
			root.registerOperationHandler(
					SearchSolutionsRequestHandler.OPERATION_NAME,
					SearchSolutionsRequestHandler.INSTANCE,
					new SearchSolutionsRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					GetSolutionRequestHandler.OPERATION_NAME,
					GetSolutionRequestHandler.INSTANCE,
					new GetSolutionRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					DiagnoseStringRequestHandler.OPERATION_NAME,
					DiagnoseStringRequestHandler.INSTANCE,
					new DiagnoseStringRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					DiagnoseFileRequestHandler.OPERATION_NAME,
					DiagnoseFileRequestHandler.INSTANCE,
					new DiagnoseFileRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					SymptomsFileRequestHandler.OPERATION_NAME,
					SymptomsFileRequestHandler.INSTANCE,
					new SymptomsFileRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					OpenCaseRequestHandler.OPERATION_NAME,
					OpenCaseRequestHandler.INSTANCE,
					new OpenCaseRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					ModifyCaseRequestHandler.OPERATION_NAME,
					ModifyCaseRequestHandler.INSTANCE,
					new ModifyCaseRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					ListProductsRequestHandler.OPERATION_NAME,
					ListProductsRequestHandler.INSTANCE,
					new ListProductsRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					GetVersionsRequestHandler.OPERATION_NAME,
					GetVersionsRequestHandler.INSTANCE,
					new GetVersionsRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					ListSeveritiesRequestHandler.OPERATION_NAME,
					ListSeveritiesRequestHandler.INSTANCE,
					new ListSeveritiesRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					ListCasesRequestHandler.OPERATION_NAME,
					ListCasesRequestHandler.INSTANCE,
					new ListCasesRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					GetCaseRequestHandler.OPERATION_NAME,
					GetCaseRequestHandler.INSTANCE,
					new GetCaseRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			root.registerOperationHandler(
					GetCommentsRequestHandler.OPERATION_NAME,
					GetCommentsRequestHandler.INSTANCE,
					new GetCommentsRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
			
			root.registerOperationHandler(
					AddCommentRequestHandler.OPERATION_NAME,
					AddCommentRequestHandler.INSTANCE,
					new AddCommentRequestHandler().getDODP(),
					EnumSet.of(Flag.RUNTIME_ONLY));
		}
		subsystemRegistration
				.registerXMLElementWriter(RedhatAccessPluginEapSubsystemParser.INSTANCE);
	}

	public void initializeParsers(ExtensionParsingContext context) {
		context.setSubsystemXmlMapping(SUBSYSTEM_NAME,
				Namespace.CURRENT.getUriString(),
				RedhatAccessPluginEapSubsystemParser.INSTANCE);
	}

	private static class RedhatAccessPluginEapDescribeHandler implements
			OperationStepHandler, DescriptionProvider {
		static final RedhatAccessPluginEapDescribeHandler INSTANCE = new RedhatAccessPluginEapDescribeHandler();

		public void execute(OperationContext context, ModelNode operation)
				throws OperationFailedException {
			ModelNode result = context.getResult();

			result.add(Util.getEmptyOperation(ADD, pathAddress(PathElement
					.pathElement(SUBSYSTEM, SUBSYSTEM_NAME))));

			context.completeStep();
		}

		public ModelNode getModelDescription(Locale locale) {
			return CommonDescriptions.getSubsystemDescribeOperation(locale);
		}
	}

	private static ModelNode pathAddress(PathElement... elements) {
		return PathAddress.pathAddress(elements).toModelNode();
	}
}
