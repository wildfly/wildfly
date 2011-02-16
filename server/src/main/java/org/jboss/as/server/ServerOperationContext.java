/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServerOperationContext extends OperationContext {

    /** {@inheritDoc} */
    ServerController getController();

    /**
     * Changes {@link ServerController#getState() the server controller state}
     * to {@link ServerController.State#RESTART_REQUIRED}, unless the current
     * state is {@link ServerController.State#STARTING}.
     */
    void restartRequired();

    /**
     * Reverts any change made by {@link #restartRequired()} unless the controller
     * state has subsequently been changed. (Following the "subsequent change"
     * the {@link ServerController#getState()} may still return
     * {@link ServerController.State#RESTART_REQUIRED};
     * the fact that a different caller also requested that it be RESTART_REQUIRED
     * in and of itself constitutes a change to the controllers state.)
     */
    void revertRestartRequired();

}
