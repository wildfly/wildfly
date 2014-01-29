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
import com.redhat.gss.redhat_support_lib.infrastructure.Comments;
import com.redhat.gss.redhat_support_lib.parsers.Case;
import com.redhat.gss.redhat_support_lib.parsers.Comment;
import com.redhat.gss.redhat_support_lib.parsers.Product;
import com.redhat.gss.redhat_support_lib.parsers.Solution;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;

public class AddCommentRequestHandler extends BaseRequestHandler implements
		OperationStepHandler, DescriptionProvider {

	public static final String OPERATION_NAME = "add-comment";
	public static final AddCommentRequestHandler INSTANCE = new AddCommentRequestHandler();

	public static final SimpleAttributeDefinition caseNumber = new SimpleAttributeDefinitionBuilder(
			"caseNumber", ModelType.STRING).setAllowExpression(true)
			.setXmlName("caseNumber")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

	public static final SimpleAttributeDefinition commentText = new SimpleAttributeDefinitionBuilder(
			"commentText", ModelType.STRING).setAllowExpression(true)
			.setXmlName("commentText")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

	public AddCommentRequestHandler() {
		super(PathElement.pathElement(OPERATION_NAME), RedhatAccessPluginEapExtension
				.getResourceDescriptionResolver(OPERATION_NAME), INSTANCE,
				INSTANCE, OPERATION_NAME, caseNumber, commentText);
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
				String caseNumberString = caseNumber.resolveModelAttribute(
						context, operation).asString();
				String commentTextString = commentText.resolveModelAttribute(
						context, operation).asString();

				Comment comment = new Comment();
				comment.setCaseNumber(caseNumberString);
				comment.setText(commentTextString);
				try {
					comment = api.getComments().add(comment);
				} catch (Exception e) {
					throw new OperationFailedException(e.getLocalizedMessage(),
							e);
				}
				ModelNode response = context.getResult();

				if (comment.getCreatedBy() != null) {
					response.get("Author").set(comment.getCreatedBy());
				}
				if (comment.getCreatedDate() != null) {
					response.get("Date").set(
							comment.getCreatedDate().getTime().toString());
				}
				if (comment.getText() != null) {
					response.get("Text").set(comment.getText());
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
