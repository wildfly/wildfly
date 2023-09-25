/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.metadata.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.property.CompositePropertyResolver;
import org.jboss.metadata.property.PropertiesPropertyResolver;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.metadata.property.PropertyReplacers;
import org.jboss.metadata.property.SimpleExpressionResolver;
import org.jboss.metadata.property.SystemPropertyResolver;

/**
 * @author John Bailey
 */
public class DeploymentPropertyResolverProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        DeploymentUnit current = deploymentUnit;
        final List<SimpleExpressionResolver> propertyResolvers = new ArrayList<>();
        final List<Function<String, String>> functions = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_EXPRESSION_RESOLVERS);
        if (functions != null) {
            for (Function<String, String> funct : functions) {
                propertyResolvers.add(expressionContent -> {
                    String input = "${" + expressionContent + "}";
                    String resolved = funct.apply(input);
                    return resolved == null ? null : new SimpleExpressionResolver.ResolutionResult(resolved, false);
                });
            }
        }
        do {
            final Properties deploymentProperties = current.getAttachment(Attachments.DEPLOYMENT_PROPERTIES);
            if (deploymentProperties != null) {
                propertyResolvers.add(new PropertiesPropertyResolver(deploymentProperties));
            }
            current = current.getParent();
        } while (current != null);
        propertyResolvers.add(SystemPropertyResolver.INSTANCE);
        final PropertyReplacer propertyReplacer = PropertyReplacers.resolvingExpressionReplacer(new CompositePropertyResolver(propertyResolvers.toArray(SimpleExpressionResolver[]::new)));
        deploymentUnit.putAttachment(Attachments.FINAL_PROPERTY_REPLACER, propertyReplacer);
    }
}
