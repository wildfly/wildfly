/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXWS_ENDPOINTS_KEY;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
import org.jboss.as.webservices.util.ASHelper;
import org.wildfly.extension.undertow.deployment.UndertowAttachments;

import io.undertow.predicate.Predicate;

/**
 * DUP for telling Undertow to let WS deal with blocking requests to
 * serve XTS requirements.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public class GracefulShutdownIntegrationProcessor implements DeploymentUnitProcessor {

    private static final AttachmentKey<Predicate> ATTACHMENT_KEY = AttachmentKey.create(Predicate.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final JAXWSDeployment wsDeployment = unit.getAttachment(JAXWS_ENDPOINTS_KEY);
        if (wsDeployment != null) {
            Predicate predicate = new AllowWSRequestPredicate(ASHelper.getJBossWebMetaData(unit));
            unit.putAttachment(ATTACHMENT_KEY, predicate);
            unit.addToAttachmentList(UndertowAttachments.ALLOW_REQUEST_WHEN_SUSPENDED, predicate);
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        Predicate predicate = deploymentUnit.removeAttachment(ATTACHMENT_KEY);
        if (predicate != null) {
            deploymentUnit.getAttachmentList(UndertowAttachments.ALLOW_REQUEST_WHEN_SUSPENDED).remove(predicate);
        }
    }

}
