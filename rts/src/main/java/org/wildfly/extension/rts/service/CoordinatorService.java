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
package org.wildfly.extension.rts.service;

import io.undertow.servlet.api.DeploymentInfo;

import java.util.HashMap;
import java.util.Map;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.rts.jaxrs.CoordinatorApplication;
import org.wildfly.extension.rts.logging.RTSLogger;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class CoordinatorService extends AbstractRTSService implements Service<CoordinatorService> {

    public static final String CONTEXT_PATH = "/rest-at-coordinator";

    private static final String DEPLOYMENT_NAME = "REST-AT Coordinator";

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
        initialParameters.put("javax.ws.rs.Application", CoordinatorApplication.class.getName());

        final DeploymentInfo coordinatorDeploymentInfo = getDeploymentInfo(DEPLOYMENT_NAME, CONTEXT_PATH, initialParameters);

        deployServlet(coordinatorDeploymentInfo);
    }

}
