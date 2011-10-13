/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.webservices.deployers;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.service.EndpointService;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.ws.common.deployment.EndpointLifecycleDeploymentAspect;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Creates Endpoint Service instance when starting the Endpoint
 *
 * @author alessio.soldano@jboss.com
 * @since 13-May-2011
 */
public final class EndpointServiceDeploymentAspect extends EndpointLifecycleDeploymentAspect {

    @Override
    public void start(Deployment dep) {
        super.start(dep);
        final ServiceTarget target = dep.getAttachment(ServiceTarget.class);
        final DeploymentUnit unit = WSHelper.getRequiredAttachment(dep, DeploymentUnit.class);
        if (target != null) {
            for (Endpoint ep : dep.getService().getEndpoints()) {
                EndpointService.install(target, ep, unit);
            }
        }
    }
}
