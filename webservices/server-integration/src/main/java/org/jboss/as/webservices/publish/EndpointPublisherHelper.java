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
package org.jboss.as.webservices.publish;

import java.util.List;
import java.util.Map;

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
            JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd) {
        EndpointPublisherImpl publisher = new EndpointPublisherImpl(null, true);
        return publisher.doPrepare(context, loader, urlPatternToClassName, jbwmd, wsmd, jbwsmd);
    }
}
