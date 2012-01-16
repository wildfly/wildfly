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

package org.jboss.as.controller.client.helpers.standalone.impl;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.client.helpers.standalone.AddDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.InitialDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ReplaceDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.UndeployDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction.Type;
import org.jboss.as.protocol.StreamUtils;

/**
 * {@link DeploymentPlanBuilder} implementation meant to handle in-VM calls.
 *
 * @author Brian Stansberry
 */
class DeploymentPlanBuilderImpl
    implements AddDeploymentPlanBuilder, InitialDeploymentPlanBuilder, UndeployDeploymentPlanBuilder  {

    private final boolean shutdown;
    private final long gracefulShutdownPeriod;
    private final boolean globalRollback;
    private volatile boolean cleanupInFinalize = true;

    private final List<DeploymentActionImpl> deploymentActions = new ArrayList<DeploymentActionImpl>();

    DeploymentPlanBuilderImpl() {
        this.shutdown = false;
        this.globalRollback = true;
        this.gracefulShutdownPeriod = -1;
    }

    DeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing) {
        this.deploymentActions.addAll(existing.deploymentActions);
        this.shutdown = existing.shutdown;
        this.globalRollback = existing.globalRollback;
        this.gracefulShutdownPeriod = existing.gracefulShutdownPeriod;
        existing.cleanupInFinalize = false;
    }

    DeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, boolean globalRollback) {
        this.deploymentActions.addAll(existing.deploymentActions);
        this.shutdown = false;
        this.globalRollback = globalRollback;
        this.gracefulShutdownPeriod = -1;
        existing.cleanupInFinalize = false;
    }

    DeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, long gracefulShutdownPeriod) {
        this.deploymentActions.addAll(existing.deploymentActions);
        this.shutdown = true;
        this.globalRollback = false;
        this.gracefulShutdownPeriod = gracefulShutdownPeriod;
        existing.cleanupInFinalize = false;
    }

    DeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentActionImpl modification) {
        this(existing);
        this.deploymentActions.add(modification);
    }

    @Override
    public DeploymentAction getLastAction() {
        return deploymentActions.size() == 0 ? null : deploymentActions.get(deploymentActions.size() - 1);
    }

    @Override
    public List<DeploymentAction> getDeploymentActions() {
        return new ArrayList<DeploymentAction>(deploymentActions);
    }

    @Override
    public long getGracefulShutdownTimeout() {
        return gracefulShutdownPeriod;
    }

    @Override
    public boolean isGlobalRollback() {
        return globalRollback;
    }

    @Override
    public boolean isGracefulShutdown() {
        return shutdown && gracefulShutdownPeriod > -1;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public DeploymentPlan build() {
        DeploymentPlan dp = new DeploymentPlanImpl(Collections.unmodifiableList(deploymentActions), globalRollback, shutdown, gracefulShutdownPeriod);
        cleanupInFinalize = false;
        return dp;
    }

    @Override
    public AddDeploymentPlanBuilder add(File file) throws IOException {
        String name = file.getName();
        return add(name, name, file.toURI().toURL());
    }

    @Override
    public AddDeploymentPlanBuilder add(URL url) throws IOException {
        String name = getName(url);
        return add(name, name, url);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, File file) throws IOException {
        return add(name, name, file.toURI().toURL());
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, URL url) throws IOException {
        return add(name, name, url);
    }

    private AddDeploymentPlanBuilder add(String name, String commonName, URL url) throws IOException {
        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream stream = conn.getInputStream();
            return add(name, commonName, stream, true);
        } catch (IOException e) {
            cleanup();
            throw e;
        }
    }

    private DeploymentPlanBuilder replace(String name, String commonName, URL url) throws IOException {
        try {
            URLConnection conn = url.openConnection();
            conn.connect();
            InputStream stream = conn.getInputStream();
            return replace(name, commonName, stream, true);
        } catch (IOException e) {
            cleanup();
            throw e;
        }
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, InputStream stream) {
        return add(name, name, stream);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, String commonName, InputStream stream) {
        return add(name, commonName, stream, false);
    }

    private AddDeploymentPlanBuilder add(String name, String commonName, InputStream stream, boolean internalStream) {
        DeploymentActionImpl mod = DeploymentActionImpl.getAddAction(name, commonName, stream, internalStream);
        return new DeploymentPlanBuilderImpl(this, mod);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.server.AddDeploymentPlanBuilder#andDeploy()
     */
    @Override
    public DeploymentPlanBuilder andDeploy() {
        String addedKey = getAddedContentKey();
        DeploymentActionImpl deployMod = DeploymentActionImpl.getDeployAction(addedKey);
        return new DeploymentPlanBuilderImpl(this, deployMod);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.deployment.client.api.server.AddDeploymentPlanBuilder#andReplace(java.lang.String)
     */
    @Override
    public ReplaceDeploymentPlanBuilder andReplace(String toReplace) {
        String newContentKey = getAddedContentKey();
        return replace(newContentKey, toReplace);
    }

    @Override
    public DeploymentPlanBuilder deploy(String key) {
        DeploymentActionImpl mod = DeploymentActionImpl.getDeployAction(key);
        return new DeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public UndeployDeploymentPlanBuilder undeploy(String key) {
        DeploymentActionImpl mod = DeploymentActionImpl.getUndeployAction(key);
        return new DeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public DeploymentPlanBuilder redeploy(String deploymentName) {
        DeploymentActionImpl mod = DeploymentActionImpl.getRedeployAction(deploymentName);
        return new DeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public ReplaceDeploymentPlanBuilder replace(String replacement, String toReplace) {
        DeploymentActionImpl mod = DeploymentActionImpl.getReplaceAction(replacement, toReplace);
        return new ReplaceDeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public DeploymentPlanBuilder replace(File file) throws IOException {
        String name = file.getName();
        return replace(name, name, file.toURI().toURL());
    }

    @Override
    public DeploymentPlanBuilder replace(URL url) throws IOException {
        String name = getName(url);
        return replace(name, name, url);
    }

    @Override
    public DeploymentPlanBuilder replace(String name, File file) throws IOException {
        return replace(name, name, file.toURI().toURL());
    }

    @Override
    public DeploymentPlanBuilder replace(String name, URL url) throws IOException {
        return replace(name, name, url);
    }

    @Override
    public DeploymentPlanBuilder replace(String name, InputStream stream) {
        return replace(name, name, stream);
    }

    @Override
    public DeploymentPlanBuilder replace(String name, String commonName, InputStream stream) {
        return replace(name, commonName, stream, false);
    }

    private DeploymentPlanBuilder replace(String name, String commonName, InputStream stream, boolean internalStream) {

        DeploymentActionImpl mod = DeploymentActionImpl.getFullReplaceAction(name, commonName, stream, internalStream);
        return new DeploymentPlanBuilderImpl(this, mod);
    }

    @Override
    public DeploymentPlanBuilder andRemoveUndeployed() {
        DeploymentAction last = getLastAction();
        if (last.getType() != Type.UNDEPLOY) {
            // Someone cast to the impl class instead of using the interface
            cleanup();
            throw MESSAGES.invalidPrecedingAction(Type.UNDEPLOY);
        }
        DeploymentActionImpl removeMod = DeploymentActionImpl.getRemoveAction(last.getDeploymentUnitUniqueName());
        return new DeploymentPlanBuilderImpl(this, removeMod);
    }

    @Override
    public DeploymentPlanBuilder remove(String key) {
        DeploymentActionImpl removeMod = DeploymentActionImpl.getRemoveAction(key);
        return new DeploymentPlanBuilderImpl(this, removeMod);
    }

    @Override
    public DeploymentPlanBuilder withRollback() {
        if (deploymentActions.size() > 0) {
            // Someone has cast to this impl class
            cleanup();
            throw MESSAGES.operationsNotAllowed(InitialDeploymentPlanBuilder.class.getSimpleName());
        }
        if (shutdown) {
            cleanup();
            throw MESSAGES.globalRollbackNotCompatible();
        }
        return new DeploymentPlanBuilderImpl(this, true);
    }

    @Override
    public DeploymentPlanBuilder withoutRollback() {
        if (deploymentActions.size() > 0) {
            // Someone has cast to this impl class
            cleanup();
            throw MESSAGES.operationsNotAllowed(InitialDeploymentPlanBuilder.class.getSimpleName());
        }
        return new DeploymentPlanBuilderImpl(this, false);
    }

    @Override
    public DeploymentPlanBuilder withGracefulShutdown(long timeout, TimeUnit timeUnit) {
        // TODO determine how to remove content. Perhaps with a signal to the
        // deployment repository service such that as part of shutdown after
        // undeploys are done it then removes the content?

        if (deploymentActions.size() > 0) {
            // Someone has to cast this impl class
            cleanup();
            throw MESSAGES.operationsNotAllowed(InitialDeploymentPlanBuilder.class.getSimpleName());
        }
        if (globalRollback) {
            cleanup();
            throw MESSAGES.globalRollbackNotCompatible();
        }
        long period = timeUnit.toMillis(timeout);
        if (shutdown && period != gracefulShutdownPeriod) {
            cleanup();
            throw MESSAGES.gracefulShutdownAlreadyConfigured(gracefulShutdownPeriod);
        }
        return new DeploymentPlanBuilderImpl(this, period);
    }

    @Override
    public DeploymentPlanBuilder withShutdown() {
        // TODO determine how to remove content. Perhaps with a signal to the
        // deployment repository service such that as part of shutdown after
        // undeploys are done it then removes the content?

        if (deploymentActions.size() > 0) {
            // Someone has to cast this impl class
            cleanup();
            throw MESSAGES.operationsNotAllowed(InitialDeploymentPlanBuilder.class.getSimpleName());
        }
        if (globalRollback) {
            cleanup();
            throw MESSAGES.globalRollbackNotCompatible();
        }
        if (shutdown && gracefulShutdownPeriod != -1) {
            cleanup();
            throw MESSAGES.gracefulShutdownAlreadyConfigured(gracefulShutdownPeriod);
        }
        return new DeploymentPlanBuilderImpl(this, -1);
    }


    private String getAddedContentKey() {
        DeploymentAction last = getLastAction();
        if (last.getType() != Type.ADD) {
            // Someone cast to the impl class instead of using the interface
            cleanup();
            throw MESSAGES.invalidPrecedingAction(Type.ADD);
        }
        return last.getDeploymentUnitUniqueName();
    }

    private String getName(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                File f = new File(url.toURI());
                return f.getName();
            } catch (URISyntaxException e) {
                cleanup();
                throw MESSAGES.invalidUri(e, url);
            }
        }

        String path = url.getPath();
        int idx = path.lastIndexOf('/');
        while (idx == path.length() - 1) {
            path = path.substring(0, idx);
            idx = path.lastIndexOf('/');
        }
        if (idx == -1) {
            cleanup();
            throw MESSAGES.cannotDeriveDeploymentName(url);
        }

        return path.substring(idx + 1);
    }

    protected void cleanup() {
        for (DeploymentActionImpl action : deploymentActions) {
            if (action.isInternalStream() && action.getContentStream() != null) {
                StreamUtils.safeClose(action.getContentStream());
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (cleanupInFinalize) {
            cleanup();
        }
    }
}
