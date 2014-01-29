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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import com.redhat.gss.extension.RedhatAccessPluginEapDescriptions;
import com.redhat.gss.extension.RedhatAccessPluginEapExtension;
import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.parsers.Case;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

public class OpenCaseRequestHandler extends BaseRequestHandler implements
		OperationStepHandler, DescriptionProvider {

	public static final String OPERATION_NAME = "open-case";
	public static final OpenCaseRequestHandler INSTANCE = new OpenCaseRequestHandler();

	public static final SimpleAttributeDefinition summary = new SimpleAttributeDefinitionBuilder(
			"Summary", ModelType.STRING).setAllowExpression(true)
			.setXmlName("Summary")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
	public static final SimpleAttributeDefinition description = new SimpleAttributeDefinitionBuilder(
			"Description", ModelType.STRING).setAllowExpression(true)
			.setXmlName("Description")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
	public static final SimpleAttributeDefinition severity = new SimpleAttributeDefinitionBuilder(
			"Severity", ModelType.STRING, true).setAllowExpression(true)
			.setXmlName("Severity")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
	public static final SimpleAttributeDefinition product = new SimpleAttributeDefinitionBuilder(
			"Product", ModelType.STRING).setAllowExpression(true)
			.setXmlName("Product")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();
	public static final SimpleAttributeDefinition version = new SimpleAttributeDefinitionBuilder(
			"Version", ModelType.STRING).setAllowExpression(true)
			.setXmlName("Version")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

	public OpenCaseRequestHandler() {
		super(PathElement.pathElement(OPERATION_NAME), RedhatAccessPluginEapExtension
				.getResourceDescriptionResolver(OPERATION_NAME), INSTANCE,
				INSTANCE, OPERATION_NAME, summary, description, severity,
				product, version);
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
				Case cas = new Case();
				cas.setSummary(summary
						.resolveModelAttribute(context, operation).asString());
				cas.setDescription(description.resolveModelAttribute(context,
						operation).asString());
				if (severity.resolveModelAttribute(context, operation)
						.isDefined()) {
					cas.setSeverity(severity.resolveModelAttribute(context,
							operation).asString());
				}
				cas.setProduct(product
						.resolveModelAttribute(context, operation).asString());
				cas.setVersion(version
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
				if(cas.getSeverity() != null){
					response.get("Severity").set(cas.getSeverity());
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
