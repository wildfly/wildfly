/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.osgi.service;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.SetupAction;

/**
 * Builds a {@link ContextManager}
 *
 * @author Stuart Douglas
 *@author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
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
    public ContextManagerBuilder add(final SetupAction action) {
        setupActions.add(action);
        return this;
    }

    public ContextManagerBuilder addAll(final DeploymentUnit deploymentUnit) {
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
