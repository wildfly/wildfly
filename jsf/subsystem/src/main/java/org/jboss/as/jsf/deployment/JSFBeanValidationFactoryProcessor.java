/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.deployment;

import static jakarta.faces.validator.BeanValidator.VALIDATOR_FACTORY_KEY;

import jakarta.validation.ValidatorFactory;

import org.jboss.as.ee.beanvalidation.BeanValidationAttachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.ServletContextAttribute;

/**
 * Deployment processor that adds the Jakarta Contexts and Dependency Injection enabled ValidatorFactory to the servlet context.
 *
 * @author Farah Juma
 */
public class JSFBeanValidationFactoryProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            return;
        }

        // Get the Jakarta Contexts and Dependency Injection enabled ValidatorFactory and add it to the servlet context
        ValidatorFactory validatorFactory = deploymentUnit.getAttachment(BeanValidationAttachments.VALIDATOR_FACTORY);
        if(validatorFactory != null) {
            deploymentUnit.addToAttachmentList(ServletContextAttribute.ATTACHMENT_KEY,
                    new ServletContextAttribute(VALIDATOR_FACTORY_KEY, validatorFactory));
        }
    }
}
