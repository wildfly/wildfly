/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.ee.metadata.property;

import java.util.List;
import java.util.function.Function;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.property.CompositePropertyResolver;
import org.jboss.metadata.property.SimpleExpressionResolver;

/**
 * Integrates any {@code Function<String, String>} instances
 * {@link org.jboss.as.server.deployment.Attachments#DEPLOYMENT_EXPRESSION_RESOLVERS attached to the deployment unit}
 * into the list used to compose the final deployment property replacer.
 */
public class FunctionalResolverProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<Function<String, String>> functions = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_EXPRESSION_RESOLVERS);
        if (functions != null) {
            SimpleExpressionResolver[] sers = new SimpleExpressionResolver[functions.size()];
            for (int i = 0; i < functions.size(); i++) {
                Function<String, String> funct = functions.get(i);
                sers[i] = expressionContent -> {
                    String input = "${" + expressionContent + "}";
                    String resolved = funct.apply(input);
                    return resolved == null ? null : new SimpleExpressionResolver.ResolutionResult(resolved, false);
                };
            }
            deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_PROPERTY_RESOLVERS, new CompositePropertyResolver(sers));
        }
    }
}
