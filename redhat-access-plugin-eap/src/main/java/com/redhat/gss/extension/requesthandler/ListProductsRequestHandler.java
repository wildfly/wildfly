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
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import com.redhat.gss.extension.RedhatAccessPluginEapExtension;
import com.redhat.gss.redhat_support_lib.api.API;
import com.redhat.gss.redhat_support_lib.parsers.Product;
import java.net.MalformedURLException;
import java.util.List;

public class ListProductsRequestHandler extends BaseRequestHandler implements
		OperationStepHandler{

	public static final String OPERATION_NAME = "list-products";
	public static final ListProductsRequestHandler INSTANCE = new ListProductsRequestHandler();

	public static SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME,
            RedhatAccessPluginEapExtension
                    .getResourceDescriptionResolver())
            .setParameters(getParameters()).build();

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
				List<Product> products = null;
				try {
					products = api.getProducts().list(null);
				} catch (Exception e) {
					throw new OperationFailedException(e.getLocalizedMessage(),
							e);
				}
				ModelNode response = context.getResult();
				int i = 0;
				for (Product product : products) {
					if (product.getName() != null) {
						ModelNode productNode = response.get(i);
						productNode.get("Product").set(product.getName());
						i++;
					}
				}

				context.stepCompleted();
			}
		}, OperationContext.Stage.RUNTIME);

		context.stepCompleted();
	}
}
