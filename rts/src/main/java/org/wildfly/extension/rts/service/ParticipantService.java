/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.rts.service;

import io.undertow.servlet.api.DeploymentInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.narayana.rest.integration.ParticipantResource;
import org.jboss.narayana.rest.integration.api.ParticipantsManagerFactory;
import org.wildfly.extension.rts.jaxrs.ParticipantApplication;
import org.wildfly.extension.rts.logging.RTSLogger;
import org.wildfly.extension.undertow.Host;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class ParticipantService extends AbstractRTSService implements Service<ParticipantService> {

    public static final String CONTEXT_PATH = ParticipantResource.BASE_PATH_SEGMENT;

    private static final String DEPLOYMENT_NAME = "REST-AT Participant";

    public ParticipantService(Supplier<Host> hostSupplier, Supplier<SocketBinding> socketBindingSupplier) {
        super(hostSupplier, socketBindingSupplier);
    }

    @Override
    public ParticipantService getValue() throws IllegalStateException, IllegalArgumentException {
        RTSLogger.ROOT_LOGGER.trace("ParticipantService.getValue");

        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        RTSLogger.ROOT_LOGGER.trace("ParticipantService.start");

        deployParticipant();
        ParticipantsManagerFactory.getInstance().setBaseUrl(getBaseUrl());
    }

    @Override
    public void stop(StopContext context) {
        RTSLogger.ROOT_LOGGER.trace("ParticipantService.stop");

        undeployServlet();
    }

    private void deployParticipant() {
        undeployServlet();

        final Map<String, String> initialParameters = new HashMap<String, String>();
        initialParameters.put("jakarta.ws.rs.Application", ParticipantApplication.class.getName());

        final DeploymentInfo participantDeploymentInfo = getDeploymentInfo(DEPLOYMENT_NAME, CONTEXT_PATH, initialParameters);

        deployServlet(participantDeploymentInfo);
    }

}
