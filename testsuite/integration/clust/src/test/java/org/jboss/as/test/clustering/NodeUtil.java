/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
        for (int i = 0; i < deployments.length; i++) {
            log.info("Deploying deployment=" + deployments[i]);
            deployer.deploy(deployments[i]);
        }
    }

    public static void undeploy(Deployer deployer, String... deployments) {
        for (int i = 0; i < deployments.length; i++) {
            log.info("Undeploying deployment=" + deployments[i]);
            deployer.undeploy(deployments[i]);
        }
    }

    public static void start(ContainerController controller, Deployer deployer, String container, String deployment) {
        try {
            log.info("Starting deployment=" + deployment + ", container=" + container);
            controller.start(container);
            deployer.deploy(deployment);
            log.info("Started deployment=" + deployment + ", container=" + container);
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public static void start(ContainerController controller, String[] containers) {
        // TODO do this in parallel.
        for (int i = 0; i < containers.length; i++) {
            try {
                log.info("Starting deployment=NONE, container=" + containers[i]);
                controller.start(containers[i]);
            } catch (Throwable e) {
                log.error(e);
            }
        }
    }

    public static void start(ContainerController controller, String container) {
        try {
            log.info("Starting deployment=NONE, container=" + container);
            controller.start(container);
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public static void stop(ContainerController controller, String[] containers) {
        for (int i = 0; i < containers.length; i++) {
            try {
                log.info("Stopping container=" + containers[i]);
                controller.stop(containers[i]);
                log.info("Stopped container=" + containers[i]);
            } catch (Throwable e) {
                log.error(e);
            }
        }
    }

    public static void stop(ContainerController controller, Deployer deployer, String container, String deployment) {
        try {
            log.info("Stopping deployment=" + deployment + ", container=" + container);
            deployer.undeploy(deployment);
            controller.stop(container);
            log.info("Stopped deployment=" + deployment + ", container=" + container);
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public static void stop(ContainerController controller, String container) {
        try {
            controller.stop(container);
            log.info("Stopped deployment=NONE, container=" + container);
        } catch (Throwable e) {
            log.error(e);
        }
    }

    private NodeUtil() {
    }
}
