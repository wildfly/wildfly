/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.ejb.client.EJBClientContext;

import java.util.Map;

/**
 * Processor that sets up the current EE client context.
 *
 * @author Stuart Douglas
 */
public class EjbClientContextSetupProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EJBClientContext context = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT);

        final EjbClientContextSetupAction setupAction = new EjbClientContextSetupAction(context);

        deploymentUnit.addToAttachmentList(Attachments.SETUP_ACTIONS, setupAction);
        deploymentUnit.addToAttachmentList(org.jboss.as.ee.component.Attachments.EE_SETUP_ACTIONS, setupAction);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }

    private static final class EjbClientContextSetupAction implements SetupAction {
        private final EJBClientContext context;

        public EjbClientContextSetupAction(final EJBClientContext context) {
            this.context = context;
        }

        @Override
        public void setup(final Map<String, Object> properties) {
            EJBClientContext.restoreCurrent(context);
        }

        @Override
        public void teardown(final Map<String, Object> properties) {
            EJBClientContext.suspendCurrent();
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
