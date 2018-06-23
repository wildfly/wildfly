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
package org.jboss.as.webservices.service;

import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.publish.EndpointPublisherHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.webservices.JBossWebservicesMetaData;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;

/**
 * WS endpoint deploy service, triggers the deployment unit processing for
 * the EndpointPublishService
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EndpointDeployService implements Service<DeploymentUnit> {

    private final ServiceName name;
    private final DeploymentUnit unit;

    private EndpointDeployService(final String context, final DeploymentUnit unit) {
        this.name = WSServices.ENDPOINT_DEPLOY_SERVICE.append(context);
        this.unit = unit;
    }

    @Override
    public DeploymentUnit getValue() {
        return unit;
    }

    public ServiceName getName() {
        return name;
    }

    @Override
    public void start(final StartContext ctx) throws StartException {
        WSLogger.ROOT_LOGGER.starting(name);
        try {
            EndpointPublisherHelper.doDeployStep(ctx.getChildTarget(), unit);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(final StopContext ctx) {
        WSLogger.ROOT_LOGGER.stopping(name);
        try {
            EndpointPublisherHelper.undoDeployStep(unit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DeploymentUnit install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String,String> urlPatternToClassName, JBossWebMetaData jbwmd, WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd) {
        return install(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd, jbwsmd, null);
    }

    public static DeploymentUnit install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String, String> urlPatternToClassName, JBossWebMetaData jbwmd,
            WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd, Map<Class<?>, Object> deploymentAttachments) {
        final DeploymentUnit unit = EndpointPublisherHelper.doPrepareStep(context, loader, urlPatternToClassName, jbwmd, wsmd, jbwsmd);
        if (deploymentAttachments != null) {
            Deployment dep = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
            for (Entry<Class<?>, Object> e : deploymentAttachments.entrySet()) {
                dep.addAttachment(e.getKey(), e.getValue());
            }
        }
        final EndpointDeployService service = new EndpointDeployService(context, unit);
        final ServiceBuilder<DeploymentUnit> builder = serviceTarget.addService(service.getName(), service);
        builder.addDependency(WSServices.CONFIG_SERVICE);
        builder.setInitialMode(Mode.ACTIVE);
        builder.install();
        return unit;
    }

}
