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

package org.jboss.as.controller.client.helpers.domain.impl;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.jboss.as.controller.client.helpers.domain.AddDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.DeployDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.DuplicateDeploymentNameException;
import org.jboss.as.controller.client.helpers.domain.RemoveDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ReplaceDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlan;
import org.jboss.as.controller.client.helpers.domain.ServerGroupDeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.domain.UndeployDeploymentPlanBuilder;


/**
 * Builder capable of creating a {@link DeploymentPlanImpl}.
 *
 * @author Brian Stansberry
 */
class DeploymentPlanBuilderImpl extends AbstractDeploymentPlanBuilder implements DeploymentPlanBuilder  {

    private final DeploymentContentDistributor deploymentDistributor;

    DeploymentPlanBuilderImpl(DeploymentContentDistributor deploymentDistributor) {
        super();
        if (deploymentDistributor == null)
            throw MESSAGES.nullVar("deploymentDistributor");
        this.deploymentDistributor = deploymentDistributor;
    }

    DeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, boolean globalRollback) {
        super(existing, globalRollback);
        this.deploymentDistributor = existing.deploymentDistributor;
    }

    DeploymentPlanBuilderImpl(DeploymentPlanBuilderImpl existing, DeploymentSetPlanImpl setPlan) {
        super(existing, setPlan);
        this.deploymentDistributor = existing.deploymentDistributor;
    }

    @Override
    public AddDeploymentPlanBuilder add(File file) throws IOException, DuplicateDeploymentNameException {
        String name = file.getName();
        return add(name, name, file.toURI().toURL());
    }

    @Override
    public AddDeploymentPlanBuilder add(URL url) throws IOException, DuplicateDeploymentNameException {
        String name = getName(url);
        return add(name, name, url);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, File file) throws IOException, DuplicateDeploymentNameException {
        return add(name, name, file.toURI().toURL());
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, URL url) throws IOException, DuplicateDeploymentNameException {
        return add(name, name, url);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, InputStream stream) throws IOException, DuplicateDeploymentNameException {
        return add(name, name, stream);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name, String commonName,
            InputStream stream) throws IOException, DuplicateDeploymentNameException {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        byte[] hash = deploymentDistributor.distributeDeploymentContent(name, commonName, stream);
        DeploymentActionImpl mod = DeploymentActionImpl.getAddAction(name, commonName, hash);
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new AddDeploymentPlanBuilderImpl(this, newSet);
    }

    @Override
    public AddDeploymentPlanBuilder add(String name) throws IOException {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        DeploymentActionImpl mod = DeploymentActionImpl.getAddAction(name, null, null);

        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new AddDeploymentPlanBuilderImpl(this, newSet);
    }

    @Override
    public DeployDeploymentPlanBuilder deploy(String key) {
        return this.deploy(key, null);
    }

    @Override
    public DeployDeploymentPlanBuilder deploy(String key, String policy) {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        DeploymentActionImpl mod = DeploymentActionImpl.getDeployAction(key, policy);
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new DeployDeploymentPlanBuilderImpl(this, newSet);
    }

    @Override
    public UndeployDeploymentPlanBuilder undeploy(String key) {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        DeploymentActionImpl mod = DeploymentActionImpl.getUndeployAction(key);
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new UndeployDeploymentPlanBuilderImpl(this, newSet);
    }

    @Override
    public DeploymentPlanBuilder redeploy(String deploymentName) {
        DeploymentActionImpl mod = DeploymentActionImpl.getRedeployAction(deploymentName);
        return getNewBuilder(mod);
    }

    @Override
    public ReplaceDeploymentPlanBuilder replace(String replacement, String toReplace) {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        DeploymentActionImpl mod = DeploymentActionImpl.getReplaceAction(replacement, toReplace);
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new ReplaceDeploymentPlanBuilderImpl(this, newSet);
    }

    @Override
    public RemoveDeploymentPlanBuilder replace(File file) throws IOException {
        String name = file.getName();
        return replace(name, name, file.toURI().toURL());
    }

    @Override
    public RemoveDeploymentPlanBuilder replace(URL url) throws IOException {
        String name = getName(url);
        return replace(name, name, url);
    }

    @Override
    public RemoveDeploymentPlanBuilder replace(String name, File file) throws IOException {
        return replace(name, name, file.toURI().toURL());
    }

    @Override
    public RemoveDeploymentPlanBuilder replace(String name, URL url) throws IOException {
        return replace(name, name, url);
    }

    @Override
    public RemoveDeploymentPlanBuilder replace(String name, InputStream stream) throws IOException {
        return replace(name, name, stream);
    }

    @Override
    public RemoveDeploymentPlanBuilder replace(String name, String commonName, InputStream stream) throws IOException {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        byte[] hash = deploymentDistributor.distributeReplacementDeploymentContent(name, commonName, stream);
        DeploymentActionImpl mod = DeploymentActionImpl.getFullReplaceAction(name, commonName, hash);
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new RemoveDeploymentPlanBuilderImpl(this, newSet);
    }

    @Override
    public RemoveDeploymentPlanBuilder remove(String key) {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        DeploymentActionImpl mod = DeploymentActionImpl.getRemoveAction(key);
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new RemoveDeploymentPlanBuilderImpl(this, newSet);
    }

    ServerGroupDeploymentPlanBuilder toServerGroup(final String serverGroupName) {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        DeploymentSetPlanImpl newSet = currentSet.storeServerGroup(new ServerGroupDeploymentPlan(serverGroupName));
        return new ServerGroupDeploymentPlanBuilderImpl(this, newSet);
    }

    private AddDeploymentPlanBuilder add(String name, String commonName, URL url) throws IOException, DuplicateDeploymentNameException {
        URLConnection conn = url.openConnection();
        conn.connect();
        InputStream stream = conn.getInputStream();
        try {
            return add(name, commonName, stream);
        }
        finally {
            try { stream.close(); } catch (Exception ignored) {}
        }
    }

    private RemoveDeploymentPlanBuilder replace(String name, String commonName, URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.connect();
        InputStream stream = conn.getInputStream();
        try {
            return replace(name, commonName, stream);
        }
        finally {
            try { stream.close(); } catch (Exception ignored) {}
        }
    }

    DeploymentPlanBuilderImpl getNewBuilder(DeploymentActionImpl mod) {
        DeploymentSetPlanImpl currentSet = getCurrentDeploymentSetPlan();
        if (currentSet.hasServerGroupPlans()) {
            throw MESSAGES.cannotAddDeploymentAction();
        }
        DeploymentSetPlanImpl newSet = currentSet.addAction(mod);
        return new DeploymentPlanBuilderImpl(this, newSet);
    }

    private static String getName(URL url) {
        if ("file".equals(url.getProtocol())) {
            try {
                File f = new File(url.toURI());
                return f.getName();
            } catch (URISyntaxException e) {
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
            throw MESSAGES.cannotDeriveDeploymentName(url);
        }

        return path.substring(idx + 1);
    }
}
