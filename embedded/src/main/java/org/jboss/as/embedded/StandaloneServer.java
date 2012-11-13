/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.embedded;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.naming.Context;

import org.jboss.as.controller.client.ModelControllerClient;

/**
 * The standalone server interface.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 17-Nov-2010
 */
public interface StandaloneServer {
    // TODO: use a DeploymentPlan
    @Deprecated
    void deploy(File file) throws IOException, ExecutionException, InterruptedException;

    /**
     * Retrieve a naming context for looking up references to session beans executing in
     * the embeddable container.
     *
     * @return The naming context.
     */
    Context getContext();

    /**
     * Retrieve an MSC service and waits for the service to be active.
     * @param timeout The amount to time (in milliseconds) to wait for the service
     * @param nameSegments The ServiceName segments
     * @return the service or null if not found
     */
    Object getService(long timeout, String ... nameSegments);

    ModelControllerClient getModelControllerClient();

    void start() throws ServerStartException;

    void stop();

    // TODO: use a DeploymentPlan
    @Deprecated
    void undeploy(File file) throws ExecutionException, InterruptedException;

}
