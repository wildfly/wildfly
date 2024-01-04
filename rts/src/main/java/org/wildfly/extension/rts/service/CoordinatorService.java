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
import org.wildfly.extension.rts.jaxrs.CoordinatorApplication;
import org.wildfly.extension.rts.logging.RTSLogger;
import org.wildfly.extension.undertow.Host;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class CoordinatorService extends AbstractRTSService implements Service<CoordinatorService> {

    public static final String CONTEXT_PATH = "/rest-at-coordinator";

    private static final String DEPLOYMENT_NAME = "REST-AT Coordinator";

    public CoordinatorService(Supplier<Host> hostSupplier, Supplier<SocketBinding> socketBindingSupplier) {
        super(hostSupplier, socketBindingSupplier);
    }

    @Override
    public CoordinatorService getValue() throws IllegalStateException, IllegalArgumentException {
        RTSLogger.ROOT_LOGGER.trace("CoordinatorService.getValue");

        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        RTSLogger.ROOT_LOGGER.trace("CoordinatorService.start");

        deployCoordinator();
    }

    @Override
    public void stop(StopContext context) {
        RTSLogger.ROOT_LOGGER.trace("CoordinatorService.stop");

        undeployServlet();
    }

    private void deployCoordinator() {
        undeployServlet();

        final Map<String, String> initialParameters = new HashMap<String, String>();
        initialParameters.put("jakarta.ws.rs.Application", CoordinatorApplication.class.getName());

        final DeploymentInfo coordinatorDeploymentInfo = getDeploymentInfo(DEPLOYMENT_NAME, CONTEXT_PATH, initialParameters);

        deployServlet(coordinatorDeploymentInfo);
    }

}
