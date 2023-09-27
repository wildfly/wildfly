/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.rts.deployment;

import org.jboss.as.jaxrs.deployment.JaxrsAttachments;
import org.jboss.as.jaxrs.deployment.ResteasyDeploymentData;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.narayana.rest.bridge.inbound.EJBExceptionMapper;
import org.jboss.narayana.rest.bridge.inbound.InboundBridgeFilter;
import org.jboss.narayana.rest.bridge.inbound.TransactionalExceptionMapper;
import org.wildfly.extension.rts.jaxrs.ImportWildflyClientGlobalTransactionFilter;

import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class InboundBridgeDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName PATH_DOT_NAME = DotName.createSimple(Path.class.getName());
    private static final DotName TRANSACTIONAL_DOT_NAME = DotName.createSimple(Transactional.class.getName());
    private static final DotName STATELESS_ATTRIBUTE_DOT_NAME = DotName.createSimple(Stateless.class.getName());
    private static final DotName STATEFUL_ATTRIBUTE_DOT_NAME = DotName.createSimple(Stateful.class.getName());

    private static final String[] PROVIDERS = new String[] {
            InboundBridgeFilter.class.getName(),
            ImportWildflyClientGlobalTransactionFilter.class.getName(),
            TransactionalExceptionMapper.class.getName(),
            EJBExceptionMapper.class.getName()
    };

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (isBridgeRequired(deploymentUnit)) {
            registerProviders(deploymentUnit);
        }
    }

    private boolean isBridgeRequired(final DeploymentUnit deploymentUnit) {
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        if (index == null) {
            return false;
        }

        final List<AnnotationInstance> pathAnnotations = index.getAnnotations(PATH_DOT_NAME);

        for (AnnotationInstance annotationInstance : pathAnnotations) {
            final Object target = annotationInstance.target();

            if (target instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) target;

                if (classInfo.annotationsMap().get(TRANSACTIONAL_DOT_NAME) != null) {
                    return true;
                }
                if (classInfo.annotationsMap().get(STATELESS_ATTRIBUTE_DOT_NAME) != null ||
                    classInfo.annotationsMap().get(STATEFUL_ATTRIBUTE_DOT_NAME) != null) {
                    return true;
                }
            }
        }

        return false;
    }

    private void registerProviders(final DeploymentUnit deploymentUnit) {
        final ResteasyDeploymentData resteasyDeploymentData = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);

        if (resteasyDeploymentData != null) {
            for (final String provider : PROVIDERS) {
                resteasyDeploymentData.getScannedProviderClasses().add(provider);
            }
        }
    }

}
