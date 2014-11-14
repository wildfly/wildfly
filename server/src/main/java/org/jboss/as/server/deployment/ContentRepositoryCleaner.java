/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.server.deployment;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL_HOST_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.operations.CleanObsoleteContentHandler;
import org.jboss.dmr.ModelNode;

/**
 * In charge with checking content references and syncing them with the content repository, removing to left over contents.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
class ContentRepositoryCleaner {

    private final ModelControllerClient client;
    private final ControlledProcessStateService controlledProcessStateService;
    private final ScheduledExecutorService scheduledExecutor;

    private long cleanInterval = 0L;
    private volatile boolean enabled;
    private final boolean server;
    private ScheduledFuture<?> cleanTask;

    private final ContentRepositoryCleanerTask cleanRunnable = new ContentRepositoryCleanerTask();

    private class ContentRepositoryCleanerTask implements Runnable {

        @Override
        public void run() {
            cleanObsoleteContent();
        }
    }

    public ContentRepositoryCleaner(ModelControllerClient client, ControlledProcessStateService controlledProcessStateService,
            ScheduledExecutorService scheduledExecutor, long interval, boolean server) {
        this.controlledProcessStateService = controlledProcessStateService;
        this.client = client;
        this.scheduledExecutor = scheduledExecutor;
        this.enabled = true;
        this.cleanInterval = interval;
        this.server = server;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getCleanInterval() {
        return cleanInterval;
    }

    /**
     * Invoke with the object monitor held
     */
    private void cancelScan() {
        if (cleanTask != null) {
            cleanTask.cancel(true);
            cleanTask = null;
        }
        try {
            client.close();
        } catch (IOException ex) {
            ServerLogger.ROOT_LOGGER.failedToStopRepositoryCleaner(ex);
        }
    }

    synchronized void startScan() {
        if (enabled) {
            cleanTask = scheduledExecutor.scheduleWithFixedDelay(cleanRunnable, cleanInterval, cleanInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * {@inheritDoc}
     */
    synchronized void stopScan() {
        this.enabled = false;
        cancelScan();
    }

    void cleanObsoleteContent() {
        if (controlledProcessStateService.getCurrentState() == ControlledProcessState.State.RUNNING) {
            try {
                PathAddress address = PathAddress.EMPTY_ADDRESS;
                if (!server) {
                    ModelNode response = client.execute(Util.getReadAttributeOperation(PathAddress.EMPTY_ADDRESS, LOCAL_HOST_NAME));
                    if (SUCCESS.equals(response.get(OUTCOME).asString()) && response.get(RESULT).isDefined()) {
                        address = address.append(HOST, response.get(RESULT).asString());
                    } else if (CANCELLED.equals(response.get(OUTCOME).asString())) {
                        return;
                    } else if (FAILED.equals(response.get(OUTCOME).asString())) {
                        error(response);
                    }
                }
                ModelNode response = client.execute(Util.createOperation(CleanObsoleteContentHandler.OPERATION_NAME, address));
                if (SUCCESS.equals(response.get(OUTCOME).asString())) {
                    if(response.get(RESULT).isDefined()) {
                        ServerLogger.ROOT_LOGGER.debug(response.get(RESULT));
                    }
                } else if (CANCELLED.equals(response.get(OUTCOME).asString())) {
                    return;
                } else if (FAILED.equals(response.get(OUTCOME).asString())) {
                    error(response);
                }
            } catch (IOException e) {
                ServerLogger.ROOT_LOGGER.failedToCleanObsoleteContent(e);
            }
        }
    }

    private void error(ModelNode response) {
        if (response.hasDefined(FAILURE_DESCRIPTION)) {
            ServerLogger.ROOT_LOGGER.failedToCleanObsoleteContent(response.get(FAILURE_DESCRIPTION).asString());
        } else {
            ServerLogger.ROOT_LOGGER.failedToCleanObsoleteContent(response.asString());
        }
    }
}
