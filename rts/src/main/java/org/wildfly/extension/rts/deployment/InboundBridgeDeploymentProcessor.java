/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import javax.ejb.TransactionAttribute;
import javax.transaction.Transactional;
import javax.ws.rs.Path;
import java.util.List;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class InboundBridgeDeploymentProcessor implements DeploymentUnitProcessor {

    private static final DotName PATH_DOT_NAME = DotName.createSimple(Path.class.getName());

    private static final DotName TRANSACTIONAL_DOT_NAME = DotName.createSimple(Transactional.class.getName());

    private static final DotName TRANSACTION_ATTRIBUTE_DOT_NAME = DotName.createSimple(TransactionAttribute.class.getName());

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

    @Override
    public void undeploy(final DeploymentUnit context) {

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

                if (classInfo.annotations().get(TRANSACTIONAL_DOT_NAME) != null) {
                    return true;
                }
                if (classInfo.annotations().get(TRANSACTION_ATTRIBUTE_DOT_NAME) != null) {
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
