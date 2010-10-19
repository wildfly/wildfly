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

package org.jboss.as.deployment.filesystem;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.as.deployment.client.api.DeploymentAction;
import org.jboss.as.deployment.client.api.DuplicateDeploymentNameException;
import org.jboss.as.deployment.client.api.server.DeploymentPlan;
import org.jboss.as.deployment.client.api.server.DeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.deployment.client.api.server.ServerDeploymentPlanResult;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerModel;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service that monitors the filesystem for deployment content and if found
 * deploys it.
 *
 * TODO get this out of the domain module.
 *
 * @author Brian Stansberry
 */
public class FileSystemDeploymentService implements Service<FileSystemDeploymentService> {

    private static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "deployment", "filesystem");

    //TODO Extenalize config
    FileFilter filter = new ExtensibleFilter();

    public static ServiceName getServiceName(String path) {
        return BASE_SERVICE_NAME.append(path);
    }

    public static BatchServiceBuilder<FileSystemDeploymentService> addService(final BatchBuilder batchBuilder, final String path, final int scanInterval, final boolean scanEnabled) {
        FileSystemDeploymentService service = new FileSystemDeploymentService(new File(path), scanInterval, scanEnabled);
        ServiceName name = getServiceName(path);

        BatchServiceBuilder<FileSystemDeploymentService> serviceBuilder = batchBuilder.addService(name, service)
            .addDependency(ServerDeploymentManager.SERVICE_NAME_LOCAL, ServerDeploymentManager.class, service.injectedDeploymentManager)
            .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class, service.injectedDeploymentRepository)
            .addDependency(ServerModel.SERVICE_NAME, ServerModel.class, service.injectedServerModel);

        // FIXME inject ScheduledExecutorService from an external service dependency
        final ScheduledExecutorService hack = Executors.newSingleThreadScheduledExecutor();
        service.injectedScheduleExecutor.inject(hack);
        return serviceBuilder;
    }

    // FIXME get this list from elsewhere
    private static final Set<String> ARCHIVES = new HashSet<String>(Arrays.asList(".jar", ".war", ".ear", ".rar", ".sar", ".beans"));

    private static final String DEPLOYED = ".deployed";

    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");

    private final InjectedValue<ScheduledExecutorService> injectedScheduleExecutor = new InjectedValue<ScheduledExecutorService>();
    private final InjectedValue<ServerDeploymentManager> injectedDeploymentManager = new InjectedValue<ServerDeploymentManager>();
    private final InjectedValue<ServerDeploymentRepository> injectedDeploymentRepository = new InjectedValue<ServerDeploymentRepository>();
    private final InjectedValue<ServerModel> injectedServerModel = new InjectedValue<ServerModel>();

    private final File deploymentDir;
    private int scanInterval = 0;
    private volatile boolean scanEnabled;
    private ScheduledFuture<?> scanTask;
    private final Lock scanLock = new ReentrantLock();
    private Set<String> deployed = new HashSet<String>();

    public FileSystemDeploymentService(final File deploymentDir, final int scanInterval, final boolean scanEnabled) {
        if (deploymentDir == null) {
            throw new IllegalArgumentException("deploymentDir is null");
        }
        if (!deploymentDir.exists()) {
            throw new IllegalArgumentException(deploymentDir.getAbsolutePath() + " does not exist");
        }
        if (!deploymentDir.isDirectory()) {
            throw new IllegalArgumentException(deploymentDir.getAbsolutePath() + " is not a directory");
        }
        if (!deploymentDir.canWrite()) {
            throw new IllegalArgumentException(deploymentDir.getAbsolutePath() + " is not writable");
        }
        this.deploymentDir = deploymentDir;
        this.scanInterval = scanInterval;
        this.scanEnabled = scanEnabled;
    }

    public int getScanInterval() {
        return scanInterval;
    }

    public synchronized void setScanInterval(int scanInterval) {
        if (scanInterval != this.scanInterval) {
            cancelScan();
        }
        this.scanInterval = scanInterval;
        startScan();
    }

    public boolean isScanEnabled() {
        return scanEnabled;
    }

    public synchronized void setScanEnabled(boolean scanEnabled) {
        if (scanEnabled != this.scanEnabled) {
            cancelScan();
        }
        this.scanEnabled = scanEnabled;
        startScan();
    }

    // ---------------------------------------------------------------  Service

    @Override
    public void start(StartContext context) throws StartException {

        // Validate injections
        String type = ScheduledExecutorService.class.getSimpleName();
        try {
            injectedScheduleExecutor.getValue();
            type = ServerDeploymentManager.class.getSimpleName();
            injectedDeploymentManager.getValue();
            type = ServerDeploymentRepository.class.getSimpleName();
            injectedDeploymentRepository.getValue();
            type = ServerModel.class.getSimpleName();
            injectedServerModel.getValue();
        }
        catch (IllegalStateException ise) {
            throw new StartException(type + "not injected");
        }

        // Build list of existing ".deployed" files
        establishDeployedContentList(deploymentDir);

        startScan();

        log.infof("Started %s for directory %s", getClass().getSimpleName(), deploymentDir.getAbsolutePath());
    }

    @Override
    public synchronized void stop(StopContext context) {
        cancelScan();
    }

    @Override
    public FileSystemDeploymentService getValue() throws IllegalStateException {
        return this;
    }

    private void establishDeployedContentList(File dir) {
        ServerModel serverModel = injectedServerModel.getValue();
        File[] children = dir.listFiles();
        for (File child : children) {
            String fileName = child.getName();
            if (child.isDirectory()) {
                // TODO special handling for exploded content
                establishDeployedContentList(child);
            }
            else if (fileName.endsWith(DEPLOYED)) {
                String deploymentName = fileName.substring(0, fileName.length() - DEPLOYED.length());
                if (serverModel.getDeployment(deploymentName) != null) {
                    // ServerModel knows about this deployment
                    deployed.add(deploymentName);
                }
                else {
                    // ServerModel doesn't know about this deployment, so the
                    // marker file needs to be removed.
                    if (!child.delete()) {
                        log.warnf("Cannot removed extraneous deployment marker file %s", fileName);
                    }
                }
            }
        }
    }

    private void scan() {

        try {
            scanLock.lockInterruptibly();
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            if (scanEnabled) { // confirm the scan is still wanted

                log.tracef("Scanning directory %s for deployment content changes", deploymentDir.getAbsolutePath());

                DeploymentPlanBuilder builder = injectedDeploymentManager.getValue().newDeploymentPlan();
                Map<String, File> foundDeployed = new HashMap<String, File>();
                Set<String> newlyAdded = new HashSet<String>();
                builder = scanDirectory(deploymentDir, builder, foundDeployed, newlyAdded);

                // Add remove actions to the plan for anything we count as
                // deployed that we didn't find on the scan
                Set<String> toRemove = new HashSet<String>(deployed);
                toRemove.removeAll(foundDeployed.keySet());
                for (String missing : toRemove) {
                    builder = builder.undeploy(missing).andRemoveUndeployed();
                }

                // Throw away any found marker files that we didn't already know about
                Set<String> validFinds = cleanSpuriousMarkerFiles(foundDeployed);
                validFinds.addAll(newlyAdded);
                this.deployed = validFinds;

                DeploymentPlan plan = builder.build();

                if (plan.getDeploymentActions().size() > 0) {
                    if (log.isDebugEnabled()) {
                        for (DeploymentAction action : plan.getDeploymentActions()) {
                            log.debugf("Deployment plan %s includes action of type %s affecting deployment %s", plan.getId(), action.getType(), action.getDeploymentUnitUniqueName());
                        }
                    }
                    Future<ServerDeploymentPlanResult> future = injectedDeploymentManager.getValue().execute(plan);

                    ServerDeploymentPlanResult result = future.get();
                    // FIXME deal with result
                }

                log.tracef("Scan complete");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted waiting on completion of deployment plan");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Retrieval of " + ServerDeploymentPlanResult.class.getName() + " threw an exception.", e);
            // FIXME any other handling?
        }
        finally {
            scanLock.unlock();
        }
    }

    private Set<String> cleanSpuriousMarkerFiles(Map<String, File> found) {
        Set<String> valid = new HashSet<String>();

        for (Map.Entry<String, File> entry : found.entrySet()) {
            if (deployed.contains(entry.getKey())) {
                valid.add(entry.getKey());
            }
            else {
                // Try and clean up
                entry.getValue().delete();
            }
        }
        return valid;
    }

    /**
     * Scan the given directory for content changes.
     *
     * @param directory the directory to scan
     * @param builder the builder to use to add deployment actions as new content is found
     * @param foundDeployed place to store marker files found in the directory; key is the name
     *           of the deployment, value is the marker file
     * @param newlyAdded place to store names of newly added content
     *
     * @return the builder the current builder following any changes
     */
    private DeploymentPlanBuilder scanDirectory(File directory, DeploymentPlanBuilder builder, Map<String, File> foundDeployed, Set<String> newlyAdded) {

        //TODO externalize config of filter?
        for (File child : directory.listFiles(filter)) {

            String fileName = child.getName();

            if (fileName.endsWith(DEPLOYED)) {
                String origName = fileName.substring(0, fileName.length() - DEPLOYED.length());
                foundDeployed.put(origName, child);
            }
            else if (child.isDirectory()) {
                int idx = fileName.lastIndexOf('.');
                if (idx > -1 && ARCHIVES.contains(fileName.substring(idx))) {
                    // FIXME handle exploded deployments
                    log.warnf("%s is an exploded deployment and exploded deployments are not currently handled by %s", child.getName(), getClass().getSimpleName());
                }
                else {
                    // It's just a dir for organizing content. Recurse
                    builder = scanDirectory(child, builder, foundDeployed, newlyAdded);
                }
            }
            else {
                // Found a single non-marker file

                DeploymentPlanBuilder currentBuilder = builder;

                boolean uploaded = false;
                boolean replace = false;
                ServerGroupDeploymentElement deployment = injectedServerModel.getValue().getDeployment(fileName);
                if (deployment != null) {
                    if (deployment.isStart()) {
                        replace = true;
                    }
                    else {
                        // A replace(child) will not result in deploying the new
                        // content, which is not the semantic we want with
                        // filesystem hot deployment. So clean out the
                        // existing deployment from the config and do a new
                        // add+deploy below
                        builder = builder.remove(fileName);
                    }
                }

                try {
                    if (replace) {
                        builder = builder.replace(child);
                    }
                    else {
                        builder = builder.add(child).andDeploy();
                    }
                    uploaded = true;
                } catch (IOException e) {
                    log.errorf(e, "Failed adding deployment content at %s", child.getAbsolutePath());
                } catch (DuplicateDeploymentNameException e) {
                    // Content with same name must have been added via some
                    // other means. Warn and replace
                    log.warnf("Deployment content with name %s is already installed " +
                            "but was unknown to this filesystem deployment scanner. " +
                            "Replacing the existing content with new content %s.", fileName, child.getAbsolutePath());
                    try {
                        builder = builder.replace(child);
                        uploaded = true;
                    } catch (IOException e1) {
                        log.errorf(e1, "Failed replacing %s with content at %s", fileName, child.getAbsolutePath());
                    }
                }

                if (uploaded) {
                    if (replaceWithDeployedMarker(child)) {
                        newlyAdded.add(fileName);
                    }
                    else {
                        // Discard the deployment plan work done in this step
                        builder = currentBuilder;
                    }
                }
            }
        }
        return builder;
    }

    /** Adds a marker file, deletes the regular content file */
    private boolean replaceWithDeployedMarker(File child) {
        boolean ok = false;
        File marker = new File(child.getParent(), child.getName() + DEPLOYED);
        FileOutputStream fos = null;
        try {
            marker.createNewFile();
            fos = new FileOutputStream(marker);
            fos.write(child.getName().getBytes());
            ok = true;
        }
        catch (IOException io) {
            log.errorf(io, "Caught exception writing deployment marker file %s", marker.getAbsolutePath());
        }
        finally {
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException ignored) {
                    log.warnf("Could not close output stream for deployment marker file %s", marker.getAbsolutePath());
                }
            }
        }

        if (ok) {
            ok = child.delete();
            if (!ok) {
                log.errorf("Cannot remove deployment content file %s", child.getAbsolutePath());
                marker.delete();
            }
        }

        return ok;
    }

    private synchronized void startScan() {

        if (scanEnabled) {

            Runnable r = new Runnable() {
                public void run() {
                    try {
                        scan();
                    } catch (RuntimeException e) {
                        log.errorf(e, "Scan of %s threw RuntimeException", deploymentDir.getAbsolutePath());
                    }
                }
            };

            if (scanInterval > 0) {
                scanTask = injectedScheduleExecutor.getValue().scheduleWithFixedDelay(r, 0, scanInterval, TimeUnit.MILLISECONDS);
            }
            else {
                scanTask = injectedScheduleExecutor.getValue().schedule(r, scanInterval, TimeUnit.MILLISECONDS);
            }
        }
    }

    /** Invoke with the object monitor held */
    private void cancelScan() {
        if (scanTask != null) {
            scanTask.cancel(false);
            scanTask = null;
        }
    }

}
