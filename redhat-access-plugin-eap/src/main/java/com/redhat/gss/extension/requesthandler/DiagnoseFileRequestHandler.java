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
import com.redhat.gss.redhat_support_lib.parsers.Link;
import java.net.MalformedURLException;
import java.util.List;


public class DiagnoseFileRequestHandler extends BaseRequestHandler implements
		OperationStepHandler{

	public static final String OPERATION_NAME = "diagnose-file";
	public static final DiagnoseFileRequestHandler INSTANCE = new DiagnoseFileRequestHandler();

	public static final SimpleAttributeDefinition DIAGNOSEFILE = new SimpleAttributeDefinitionBuilder(
			"diagnose-file", ModelType.STRING).setAllowExpression(true)
			.setXmlName("diagnose-file")
			.setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

	public static SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginEapExtension
                    .getResourceDescriptionResolver())
            .setParameters(getParameters(DIAGNOSEFILE)).build();

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
				String diagnoseFileString = DIAGNOSEFILE.resolveModelAttribute(
						context, operation).asString();
				List<Link> links = null;
				try {
					links = api.getProblems().diagnoseFile(diagnoseFileString);
				} catch (Exception e) {
					throw new OperationFailedException(e.getLocalizedMessage(),
							e);
				}
				ModelNode response = context.getResult();
				int i = 0;
				for (Link link : links) {
					if (link.getUri() != null) {
						String[] splitUri = link.getUri().split("/");
						String id = splitUri[splitUri.length -1];
						ModelNode solutionNode = response.get(i);

						solutionNode.get("ID").set(id);

						if (link.getValue() != null) {
							solutionNode.get("Title").set(link.getValue());
						}
						solutionNode.get("URI").set(link.getUri());
						i++;
					}
				}
				context.stepCompleted();
			}
		}, OperationContext.Stage.RUNTIME);

		context.stepCompleted();
	}
}
