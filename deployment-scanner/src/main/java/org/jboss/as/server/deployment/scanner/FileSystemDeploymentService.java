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

package org.jboss.as.server.deployment.scanner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.controller.client.ExecutionContextBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.deployment.api.DeploymentRepository;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.dmr.ModelNode;
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

//    private final ServerModel serverModel;
    private final ScheduledExecutorService scheduledExecutor;
    private final ServerController serverController;
    private final DeploymentRepository deploymentRepository;

    //TODO Extenalize filter config
    private FileFilter filter = new ExtensibleFilter();

    FileSystemDeploymentService(final File deploymentDir, final long scanInterval, final ServerController serverController, final ScheduledExecutorService scheduledExecutor, DeploymentRepository deploymentRepository) throws OperationFailedException {
        if (scheduledExecutor == null) {
            throw new IllegalStateException("null scheduled executor");
        }
        if (serverController == null) {
            throw new IllegalStateException("null server controller");
        }
        if (deploymentRepository == null) {
            throw new IllegalStateException("null deployment repository");
        }
        if (deploymentDir == null) {
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
        this.deploymentDir = deploymentDir;
        this.scanInterval = scanInterval;
        this.serverController = serverController;
        this.scheduledExecutor = scheduledExecutor;
        this.deploymentRepository = deploymentRepository;

        // Build list of existing ".deployed" files
        establishDeployedContentList(deploymentDir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public long getScanInterval() {
        return scanInterval;
    }

    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void startScanner() {
        final boolean scanEnabled = this.scanEnabled;
        if (scanEnabled) {
            return;
        }
        this.scanEnabled = true;
        startScan();
        log.infof("Started %s for directory %s", getClass().getSimpleName(), deploymentDir.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stopScanner() {
        this.scanEnabled = false;
        cancelScan();
    }

    // ---------------------------------------------------------------  protected

    void validate() {


    }

    private void establishDeployedContentList(File dir) throws OperationFailedException {
        Set<String> deploymentNames = getDeploymentNames();
        File[] children = dir.listFiles();
        for (File child : children) {
            String fileName = child.getName();
            if (child.isDirectory()) {
                // TODO special handling for exploded content
                establishDeployedContentList(child);
            } else if (fileName.endsWith(DEPLOYED)) {
                String deploymentName = fileName.substring(0, fileName.length() - DEPLOYED.length());
                if (deploymentNames.contains(deploymentName)) {
                    // ServerModel knows about this deployment
                    deployed.add(deploymentName);
                } else {
                    // ServerController doesn't know about this deployment, so the
                    // marker file needs to be removed.
                    if (!child.delete()) {
                        log.warnf("Cannot removed extraneous deployment marker file %s", fileName);
                    }
                }
            }
        }
    }

    private void scan() throws OperationFailedException {

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

                final List<ModelNode> updates = new ArrayList<ModelNode>();

                Map<String, File> foundDeployed = new HashMap<String, File>();
                Set<String> newlyAdded = new HashSet<String>();
                Set<String> registeredDeployments = getDeploymentNames();
                scanDirectory(deploymentDir, updates, foundDeployed, newlyAdded, registeredDeployments);

                // Add remove actions to the plan for anything we count as
                // deployed that we didn't find on the scan
                Set<String> toRemove = new HashSet<String>(deployed);
                toRemove.removeAll(foundDeployed.keySet());
                toRemove.removeAll(newlyAdded); // in case user removed the marker and added replacement
                for (String missing : toRemove) {
                    updates.add(getUndeployOperation(missing));
                    updates.add(getRemoveOperation(missing));
                }

                if (updates.size() > 0) {
                    if (log.isDebugEnabled()) {
                        for (ModelNode update : updates) {
                            log.debugf("Deployment scan of [%s] found update action [%s]", deploymentDir, update);
                        }
                    }
                    ExecutionContext composite = getCompositeUpdate(updates);
                    ModelNode results = serverController.execute(composite);
//                    System.out.println(composite);
//                    System.out.println(results);
                    // FIXME deal with result
                }

                // Throw away any found marker files that we didn't already know about
                Set<String> validFinds = cleanSpuriousMarkerFiles(foundDeployed);
                validFinds.addAll(newlyAdded);
                this.deployed = validFinds;

                log.tracef("Scan complete");
            }
        } finally {
            scanLock.unlock();
        }
    }

    private Set<String> cleanSpuriousMarkerFiles(Map<String, File> found) {
        Set<String> valid = new HashSet<String>();

        for (Map.Entry<String, File> entry : found.entrySet()) {
            if (deployed.contains(entry.getKey())) {
                valid.add(entry.getKey());
            } else {
                // Try and clean up
                entry.getValue().delete();
            }
        }
        return valid;
    }

    /**
     * Scan the given directory for content changes.
     *
     * @param directory     the directory to scan
     * @param updates       the update list;
     * @param foundDeployed place to store marker files found in the directory; key is the name
     *                      of the deployment, value is the marker file
     * @param newlyAdded    place to store names of newly added content
     * @param registeredDeployments TODO
     * @return the builder the current builder following any changes
     */
    private void scanDirectory(File directory, final List<ModelNode> updates, Map<String, File> foundDeployed, Set<String> newlyAdded, Set<String> registeredDeployments) {

        //TODO externalize config of filter?
        File[] children = directory.listFiles(filter);
        if (children == null) {
            return;
        }

        for (File child : children) {

            String fileName = child.getName();

            if (fileName.endsWith(DEPLOYED)) {
                String origName = fileName.substring(0, fileName.length() - DEPLOYED.length());
                foundDeployed.put(origName, child);
            } else if (child.isDirectory()) {
                int idx = fileName.lastIndexOf('.');
                if (idx > -1 && ARCHIVES.contains(fileName.substring(idx))) {
                    // FIXME handle exploded deployments
                    log.warnf("%s is an exploded deployment and exploded deployments are not currently handled by %s", child.getName(), getClass().getSimpleName());
                } else {
                    // It's just a dir for organizing content. Recurse
                    scanDirectory(child, updates, foundDeployed, newlyAdded, registeredDeployments);
                }
            } else {
                // Found a single non-marker file
                boolean uploaded = false;
                if (registeredDeployments.contains(fileName)) {

                    byte[] hash = new byte[0];
                    try {
                        final InputStream inputStream = new FileInputStream(child);
                        hash = deploymentRepository.addDeploymentContent(fileName, fileName, inputStream);
                    } catch (IOException e) {
                        log.error("Failed to add content to deployment repository for [" + fileName + "]", e);
                        continue;
                    }
                    updates.add(getFullReplaceOperation(fileName, hash));
                    uploaded = true;
                } else {
                    byte[] hash = new byte[0];
                    try {
                        final InputStream inputStream = new FileInputStream(child);
                        hash = deploymentRepository.addDeploymentContent(fileName, fileName, inputStream);
                    } catch (IOException e) {
                        log.error("Failed to add content to deployment repository for [" + fileName + "]", e);
                        continue;
                    }
                    updates.add(getAddOperation(fileName, hash));
                    updates.add(getDeployOperation(fileName));
                    uploaded = true;
                }

                if (uploaded && replaceWithDeployedMarker(child)) {
                    newlyAdded.add(fileName);
                }
            }
        }
    }

    /**
     * Adds a marker file, deletes the regular content file
     */
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
                @Override
                public void run() {
                    try {
                        scan();
                    } catch (Exception e) {
                        log.errorf(e, "Scan of %s threw Exception", deploymentDir.getAbsolutePath());
                    }
                }
            };

            if (scanInterval > 0) {
                scanTask = scheduledExecutor.scheduleWithFixedDelay(r, 0, scanInterval, TimeUnit.MILLISECONDS);
            } else {
                scanTask = scheduledExecutor.schedule(r, scanInterval, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Invoke with the object monitor held
     */
    private void cancelScan() {
        if (scanTask != null) {
            scanTask.cancel(false);
            scanTask = null;
        }
    }

    private Set<String> getDeploymentNames() throws CancellationException, OperationFailedException {
        ModelNode op = Util.getEmptyOperation(READ_CHILDREN_NAMES_OPERATION, new ModelNode());
        op.get(CHILD_TYPE).set(DEPLOYMENT);
        ModelNode response = serverController.execute(ExecutionContextBuilder.Factory.create(op).build());
        // TODO use the proper response structure
        ModelNode result = response.get("result");
        Set<String> deploymentNames = new HashSet<String>();
        if (result.isDefined()) {
            List<ModelNode> deploymentNodes = result.asList();
            for (ModelNode node : deploymentNodes) {
                deploymentNames.add(node.asString());
            }
        }
        return deploymentNames;
    }

    private ModelNode getFullReplaceOperation(String fileName, byte[] hash) {
        ModelNode op = Util.getEmptyOperation(DeploymentFullReplaceHandler.OPERATION_NAME, new ModelNode());
        op.get(NAME).set(fileName);
        op.get(HASH).set(hash);
        return op;
    }

    private ExecutionContext getCompositeUpdate(List<ModelNode> updates) {
        ModelNode op = Util.getEmptyOperation("composite", new ModelNode()); // TODO use constant
        ModelNode steps = op.get("steps");
        for (ModelNode update : updates) {
            steps.add(update);
        }
        return ExecutionContextBuilder.Factory.create(op).build();
    }

    private ModelNode getRemoveOperation(String missing) {
        ModelNode address = new ModelNode().add(DEPLOYMENT, missing);
        return Util.getEmptyOperation(DeploymentRemoveHandler.OPERATION_NAME, address);
    }

    private ModelNode getUndeployOperation(String missing) {
        ModelNode address = new ModelNode().add(DEPLOYMENT, missing);
        return Util.getEmptyOperation(DeploymentUndeployHandler.OPERATION_NAME, address);
    }

    private ModelNode getDeployOperation(String fileName) {
        ModelNode address = new ModelNode().add(DEPLOYMENT, fileName);
        return Util.getEmptyOperation(DeploymentDeployHandler.OPERATION_NAME, address);
    }

    private ModelNode getAddOperation(String fileName, byte[] hash) {
        ModelNode address = new ModelNode().add(DEPLOYMENT, fileName);
        ModelNode op = Util.getEmptyOperation(DeploymentAddHandler.OPERATION_NAME, address);
        op.get(HASH).set(hash);
        return op;
    }

}
