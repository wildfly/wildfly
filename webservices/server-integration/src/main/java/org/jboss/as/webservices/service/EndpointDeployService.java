/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.service;

import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.publish.EndpointPublisherHelper;
import org.jboss.as.webservices.util.WSAttachmentKeys;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
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
public final class EndpointDeployService implements Service {

    private final ServiceName name;
    private final DeploymentUnit unit;

    private EndpointDeployService(final String context, final DeploymentUnit unit) {
        this.name = WSServices.ENDPOINT_DEPLOY_SERVICE.append(context);
        this.unit = unit;
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
        return install(serviceTarget, context, loader, hostName, urlPatternToClassName, jbwmd, wsmd, jbwsmd, null, null);
    }

    public static DeploymentUnit install(final ServiceTarget serviceTarget, final String context, final ClassLoader loader,
            final String hostName, final Map<String, String> urlPatternToClassName, JBossWebMetaData jbwmd,
            WebservicesMetaData wsmd, JBossWebservicesMetaData jbwsmd, Map<Class<?>, Object> deploymentAttachments,
            CapabilityServiceSupport capabilityServiceSupport) {
        final DeploymentUnit unit = EndpointPublisherHelper.doPrepareStep(context, loader, urlPatternToClassName, jbwmd, wsmd,
                                                                          jbwsmd, capabilityServiceSupport);
        if (deploymentAttachments != null) {
            Deployment dep = unit.getAttachment(WSAttachmentKeys.DEPLOYMENT_KEY);
            for (Entry<Class<?>, Object> e : deploymentAttachments.entrySet()) {
                dep.addAttachment(e.getKey(), e.getValue());
            }
        }
        final EndpointDeployService service = new EndpointDeployService(context, unit);
        final ServiceBuilder builder = serviceTarget.addService(service.getName());
        builder.requires(WSServices.CONFIG_SERVICE);
        builder.setInstance(service);
        builder.install();
        return unit;
    }

}
