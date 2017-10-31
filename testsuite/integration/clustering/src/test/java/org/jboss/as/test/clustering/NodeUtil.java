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
package org.jboss.as.test.clustering;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.as.arquillian.api.WildFlyContainerController;
import org.jboss.logging.Logger;

/**
 * Utility class to start and stop containers and/or deployments.
 *
 * @author Radoslav Husar
 * @version Oct 2012
 */
public final class NodeUtil {

    private static final Logger log = Logger.getLogger(NodeUtil.class);

    public static void deploy(Deployer deployer, String... deployments) {
        for (String deployment : deployments) {
            deploy(deployer, deployment);
        }
    }

    public static void deploy(Deployer deployer, String deployment) {
        log.tracef("Deploying %s", deployment);
        deployer.deploy(deployment);
        log.tracef("Deployed %s", deployment);
    }

    public static void undeploy(Deployer deployer, String... deployments) {
        for (String deployment : deployments) {
            undeploy(deployer, deployment);
        }
    }

    public static void undeploy(Deployer deployer, String deployment) {
        log.tracef("Undeploying %s", deployment);
        deployer.undeploy(deployment);
        log.tracef("Undeployed %s", deployment);
    }

    public static void start(ContainerController controller, String... containers) {
        // TODO do this in parallel.
        for (String container : containers) {
            start(controller, container);
        }
    }

    public static void start(ContainerController controller, String container) {
        if (!controller.isStarted(container)) {
            log.tracef("Starting container %s", container);
            controller.start(container);
            log.tracef("Started container %s", container);
        } else {
            log.tracef("Container %s was already started", container);
        }
    }

    public static void stop(ContainerController controller, String... containers) {
        for (String container : containers) {
            stop(controller, container);
        }
    }

    public static void stop(ContainerController controller, String container) {
        if (controller.isStarted(container)) {
            log.tracef("Stopping container %s", container);
            controller.stop(container);
            log.tracef("Stopped container %s", container);
        } else {
            log.tracef("Container %s was already stopped", container);
        }
    }

    public static void stop(WildFlyContainerController controller, int timeout, String... containers) {
        for (String container : containers) {
            stop(controller, timeout, container);
        }
    }

    public static void stop(WildFlyContainerController controller, int timeout, String container) {
        if (controller.isStarted(container)) {
            log.tracef("Stopping container %s", container);
            controller.stop(container, timeout);
            log.tracef("Stopped container %s", container);
        } else {
            log.tracef("Container %s was already stopped", container);
        }
    }

    private NodeUtil() {
    }
}
