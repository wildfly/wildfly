/*
 * Copyright (C) 2018 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.wildfly.extension.eesecurity;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.DotName;

class EESecurityAnnotationProcessor implements DeploymentUnitProcessor {

    static final AttachmentKey<Boolean> SECURITY_PRESENT = AttachmentKey.create(Boolean.class);

    static final DotName[] ANNOTATIONS = {
            DotName.createSimple("javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition"),
            DotName.createSimple("javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition"),
            DotName.createSimple("javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition"),
            DotName.createSimple("javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition"),
            DotName.createSimple("javax.security.enterprise.identitystore.LdapIdentityStoreDefinition")
    };

    static final DotName[] INTERFACES = {
            DotName.createSimple("javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism"),
            DotName.createSimple("javax.security.enterprise.identitystore.IdentityStoreHandler"),
            DotName.createSimple("javax.security.enterprise.identitystore.IdentityStore"),
            DotName.createSimple("javax.security.enterprise.identitystore.RememberMeIdentityStore")
    };

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (DotName annotation : ANNOTATIONS) {
            if (!index.getAnnotations(annotation).isEmpty()) {
                markAsEESecurity(deploymentUnit);
                return;
            }
        }
        for (DotName annotation : INTERFACES) {
            if (!index.getAllKnownImplementors(annotation).isEmpty()) {
                markAsEESecurity(deploymentUnit);
                return;
            }
        }

    }

    private void markAsEESecurity(DeploymentUnit deploymentUnit) {
        DeploymentUnit top = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        top.putAttachment(SECURITY_PRESENT, true);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
