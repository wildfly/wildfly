/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.publish;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.host.WebHost;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;
import org.jboss.wsf.spi.publish.Context;

/**
 * Helper methods for publishing endpoints
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 *
 */
public class EndpointPublisherHelper {

    public static Context doPublishStep(WebHost host, ServiceTarget target, DeploymentUnit unit) throws Exception {
        EndpointPublisherImpl publisher = new EndpointPublisherImpl(host, true);
        return publisher.doPublish(target, unit);
    }

    public static void undoPublishStep(WebHost host, Context wsctx) throws Exception {
        List<Endpoint> eps = wsctx.getEndpoints();
        if (eps == null || eps.isEmpty()) {
            return;
        }
        EndpointPublisherImpl publisher = new EndpointPublisherImpl(host, true);
        publisher.stopWebApp(eps.get(0).getService().getDeployment());
    }

    public static void doDeployStep(ServiceTarget target, DeploymentUnit unit) {
        EndpointPublisherImpl publisher = new EndpointPublisherImpl(true);
        publisher.doDeploy(target, unit);
    }

    public static void undoDeployStep(DeploymentUnit unit) throws Exception {
        EndpointPublisherImpl publisher = new EndpointPublisherImpl(true);
        publisher.undeploy(unit);
    }

    public static DeploymentUnit doPrepareStep(String context, ClassLoader loader, Map<String, String> urlPatternToClassName,
            JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd, CapabilityServiceSupport capabilityServiceSupport) {
        EndpointPublisherImpl publisher = new EndpointPublisherImpl(null, true);
        return publisher.doPrepare(context, loader, urlPatternToClassName, jbwmd, wsmd, jbwsmd, capabilityServiceSupport);
    }
}
