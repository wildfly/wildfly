/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.deployers;

import static org.jboss.as.webservices.util.WSAttachmentKeys.JAXWS_ENDPOINTS_KEY;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.webservices.metadata.model.JAXWSDeployment;
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
            Predicate predicate = new AllowWSRequestPredicate();
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
