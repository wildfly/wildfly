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

package org.jboss.as.server.standalone.deployment;

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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.server.deployment.scanner.DeploymentScanner;
import org.jboss.as.model.ServerGroupDeploymentElement;
import org.jboss.as.model.ServerModel;
import org.jboss.as.standalone.client.api.deployment.DeploymentAction;
import org.jboss.as.standalone.client.api.deployment.DeploymentPlan;
import org.jboss.as.standalone.client.api.deployment.DeploymentPlanBuilder;
import org.jboss.as.standalone.client.api.deployment.DuplicateDeploymentNameException;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.standalone.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.logging.Logger;

/**
 * Service that monitors the filesystem for deployment content and if found
 * deploys it.
 *
 * @author Brian Stansberry
 */
class FileSystemDeploymentService implements DeploymentScanner {
    // FIXME get this list from elsewhere
    private static final Set<String> ARCHIVES = new HashSet<String>(Arrays.asList(".jar", ".war", ".ear", ".rar", ".sar", ".beans"));
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");
    private static final String DEPLOYED = ".deployed";

    private File deploymentDir;
    private long scanInterval = 0;
    private volatile boolean scanEnabled = false;
    private ScheduledFuture<?> scanTask;
    private final Lock scanLock = new ReentrantLock();
    private Set<String> deployed = new HashSet<String>();

    private ServerModel serverModel;
    private ServerDeploymentManager deploymentManager;
    private ScheduledExecutorService scheduledExecutor;

    //TODO Extenalize filter config
    private FileFilter filter = new ExtensibleFilter();

    FileSystemDeploymentService(final File deploymentDir, final long scanInterval) {
        this.deploymentDir = deploymentDir;
        this.scanInterval = scanInterval;
    }

    /** {@inheritDoc} */
    public boolean isEnabled() {
        return false;
    }

    public long getScanInterval() {
        return scanInterval;
    }

    public synchronized void setScanInterval(long scanInterval) {
        if (scanInterval != this.scanInterval) {
            cancelScan();
        }
        this.scanInterval = scanInterval;
        startScan();
    }

    public boolean isScanEnabled() {
        return scanEnabled;
    }

    /** {@inheritDoc} */
    public synchronized void startScanner() {
        final boolean scanEnabled = this.scanEnabled;
        if(scanEnabled) {
            return;
        }
        this.scanEnabled = true;
        startScan();
    }

    /** {@inheritDoc} */
    public synchronized void stopScanner() {
        this.scanEnabled = false;
        cancelScan();
    }


    // ---------------------------------------------------------------  protected

    void setDeploymentManager(ServerDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }

    void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    void setServerModel(ServerModel serverModel) {
        this.serverModel = serverModel;
    }

    void validateAndCreate() {

        if(scheduledExecutor == null) {
            throw new IllegalStateException("null scheduled executor");
        }
        if(deploymentManager == null) {
            throw new IllegalStateException("null deployment manager");
        }
        if(serverModel == null) {
            throw new IllegalStateException("null server model");
        }
        if(deploymentDir == null) {
            throw new IllegalStateException("null deployment dir");
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
        // Build list of existing ".deployed" files
        establishDeployedContentList(deploymentDir);

        startScan();

        log.infof("Started %s for directory %s", getClass().getSimpleName(), deploymentDir.getAbsolutePath());
    }

    private void establishDeployedContentList(File dir) {
        ServerModel serverModel = this.serverModel;
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

                DeploymentPlanBuilder builder = this.deploymentManager.newDeploymentPlan();
                Map<String, File> foundDeployed = new HashMap<String, File>();
                Set<String> newlyAdded = new HashSet<String>();
                builder = scanDirectory(deploymentDir, builder, foundDeployed, newlyAdded);

                // Add remove actions to the plan for anything we count as
                // deployed that we didn't find on the scan
                Set<String> toRemove = new HashSet<String>(deployed);
                toRemove.removeAll(foundDeployed.keySet());
                toRemove.removeAll(newlyAdded); // in case user removed the marker and added replacement
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
                    Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);

                    try {
                        ServerDeploymentPlanResult result = future.get(60, TimeUnit.SECONDS);
                        // FIXME deal with result
                    } catch (TimeoutException e) {
                        // This could be a WARN but deployments could validly take over 60 seconds
                        log.infof("Deployment plan %s did not complete within 60 seconds. Resuming scanning for deployment changes.", plan.getId());
                    }
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
        File[] children = directory.listFiles(filter);
        if (children == null) {
            return builder;
        }

        for (File child : children) {

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
                ServerGroupDeploymentElement deployment = serverModel.getDeployment(fileName);
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
                scanTask = scheduledExecutor.scheduleWithFixedDelay(r, 0, scanInterval, TimeUnit.MILLISECONDS);
            }
            else {
                scanTask = scheduledExecutor.schedule(r, scanInterval, TimeUnit.MILLISECONDS);
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
