/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.arquillian.context;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.SetupAction;

/**
 * Builds a {@link ContextManager}
 *
 * @author Stuart Douglas
 *
 */
public class ContextManagerBuilder {


    private final List<SetupAction> setupActions = new ArrayList<SetupAction>();

    /**
     * Adds a {@link SetupAction} to the builder. This action will be run by the {@link ContextManager} in the order it was
     * added to the builder.
     *
     * @param action The {@link SetupAction} to add to the builder
     * @return this
     */
    public ContextManagerBuilder add(SetupAction action) {
        setupActions.add(action);
        return this;
    }

    public ContextManagerBuilder addAll(DeploymentUnit deploymentUnit) {
        List<SetupAction> actions = deploymentUnit.getAttachment(Attachments.SETUP_ACTIONS);
        if (actions != null) {
            setupActions.addAll(actions);
        }
        return this;
    }

    public ContextManager build() {
        return new ContextManager(setupActions);
    }

}
