/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import static org.jboss.ws.common.integration.WSHelper.getOptionalAttachment;
import static org.jboss.ws.common.integration.WSHelper.getRequiredAttachment;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.service.EndpointService;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Creates Endpoint Service instance when starting the Endpoint
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public final class EndpointServiceDeploymentAspect extends AbstractDeploymentAspect implements Cloneable {

    private boolean stopServices = false;

    @Override
    public void start(final Deployment dep) {
        final ServiceTarget target = getOptionalAttachment(dep, ServiceTarget.class);
        final DeploymentUnit unit = getRequiredAttachment(dep, DeploymentUnit.class);
        for (final Endpoint ep : dep.getService().getEndpoints()) {
            EndpointService.install(target, ep, unit);
        }
    }

    @Override
    public void stop(Deployment dep) {
        for (final Endpoint ep : dep.getService().getEndpoints()) {
            if (ep.getLifecycleHandler() != null) {
                ep.getLifecycleHandler().stop(ep);
            }
            if (stopServices) {
                final DeploymentUnit unit = getRequiredAttachment(dep, DeploymentUnit.class);
                EndpointService.uninstall(ep, unit);
            }
        }
    }

    public void setStopServices(boolean stopServices) {
        this.stopServices = stopServices;
    }

    public Object clone() {
        EndpointServiceDeploymentAspect clone = new EndpointServiceDeploymentAspect();
        clone.setLast(isLast());
        clone.setProvides(getProvides());
        clone.setRelativeOrder(getRelativeOrder());
        clone.setRequires(getRequires());
        clone.setStopServices(stopServices);
        return clone;
    }
}
