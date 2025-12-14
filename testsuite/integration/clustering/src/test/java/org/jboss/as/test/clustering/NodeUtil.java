/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering;

import java.util.Set;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.as.arquillian.api.WildFlyContainerController;
import org.jboss.logging.Logger;

/**
 * Utility class to start and stop containers and/or deployments.
 *
 * @author Radoslav Husar
 */
public final class NodeUtil {

    private static final Logger log = Logger.getLogger(NodeUtil.class);

    public static void deploy(Deployer deployer, Set<String> deployments) {
        // n.b. fix the deployment order
        for (String deployment : deployments.stream().sorted().toList()) {
            deploy(deployer, deployment);
        }
    }

    public static void deploy(Deployer deployer, String deployment) {
        log.tracef("Deploying %s", deployment);
        deployer.deploy(deployment);
        log.tracef("Deployed %s", deployment);
    }

    public static void undeploy(Deployer deployer, Set<String> deployments) {
        for (String deployment : deployments) {
            undeploy(deployer, deployment);
        }
    }

    public static void undeploy(Deployer deployer, String deployment) {
        log.tracef("Undeploying %s", deployment);
        deployer.undeploy(deployment);
        log.tracef("Undeployed %s", deployment);
    }

    public static void start(ContainerController controller, Set<String> containers) {
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

    public static void stop(ContainerController controller, Set<String> containers) {
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

    public static boolean isStarted(ContainerController controller, String container) {
        return controller.isStarted(container);
    }

    public static void stop(WildFlyContainerController controller, Set<String> containers, int suspendTimeout) {
        for (String container : containers) {
            stop(controller, container, suspendTimeout);
        }
    }

    public static void stop(WildFlyContainerController controller, String container, int suspendTimeout) {
        if (controller.isStarted(container)) {
            log.tracef("Stopping container %s", container);
            controller.stop(container, suspendTimeout);
            log.tracef("Stopped container %s", container);
        } else {
            log.tracef("Container %s was already stopped", container);
        }
    }

    private NodeUtil() {
    }
}
