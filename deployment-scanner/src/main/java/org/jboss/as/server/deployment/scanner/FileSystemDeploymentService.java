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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.Closeable;
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
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.deployment.DeploymentAddHandler;
import org.jboss.as.server.deployment.DeploymentDeployHandler;
import org.jboss.as.server.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.server.deployment.DeploymentRedeployHandler;
import org.jboss.as.server.deployment.DeploymentRemoveHandler;
import org.jboss.as.server.deployment.DeploymentUndeployHandler;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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
    static final String DEPLOYED = ".isdeployed";
    static final String FAILED_DEPLOY = ".faileddeploy";
    static final String DO_DEPLOY = ".dodeploy";
    static final String DEPLOYING = ".deploying";
    static final String UNDEPLOYING = ".undeploying";
    static final String UNDEPLOYED = ".undeployed";

    private File deploymentDir;
    private long scanInterval = 0;
    private volatile boolean scanEnabled = false;
    private ScheduledFuture<?> scanTask;
    private final Lock scanLock = new ReentrantLock();

    private final Map<String, DeploymentMarker> deployed = new HashMap<String, DeploymentMarker>();

    private final ScheduledExecutorService scheduledExecutor;
    private final ServerController serverController;
    private final ServerDeploymentRepository deploymentRepository;

    //TODO Externalize filter config
    private FileFilter filter = new ExtensibleFilter();

    FileSystemDeploymentService(final File deploymentDir, final long scanInterval, final ServerController serverController, final ScheduledExecutorService scheduledExecutor, ServerDeploymentRepository deploymentRepository) throws OperationFailedException {
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

    private void establishDeployedContentList(File dir) throws OperationFailedException {
        final Set<String> deploymentNames = getDeploymentNames();
        final File[] children = dir.listFiles();
        for (File child : children) {
            final String fileName = child.getName();
            if (child.isDirectory()) {
                final int idx = fileName.lastIndexOf('.');
                if (idx == -1 || !ARCHIVES.contains(fileName.substring(idx))) {
                    establishDeployedContentList(child);
                }
            } else if (fileName.endsWith(DEPLOYED)) {
                final String deploymentName = fileName.substring(0, fileName.length() - DEPLOYED.length());
                if (deploymentNames.contains(deploymentName)) {
                    deployed.put(deploymentName, new DeploymentMarker(child.lastModified()));
                } else {
                    if (!child.delete()) {
                        log.warnf("Cannot removed extraneous deployment marker file %s", fileName);
                    }
                }
            }
        }
    }

    /** This method isn't private solely to allow a unit test in the same package to call it */
    void scan() {

        try {
            scanLock.lockInterruptibly();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            if (scanEnabled) { // confirm the scan is still wanted
                log.tracef("Scanning directory %s for deployment content changes", deploymentDir.getAbsolutePath());

                List<ScannerTask> scannerTasks = new ArrayList<ScannerTask>();

                final Set<String> registeredDeployments = getDeploymentNames();
                final Set<String> toRemove = new HashSet<String>(deployed.keySet());
                scanDirectory(deploymentDir, scannerTasks, registeredDeployments, toRemove);

                // Add remove actions to the plan for anything we count as
                // deployed that we didn't find on the scan
                for (String missing : toRemove) {
                    // TODO -- minor -- this assumes the deployment was in the root deploymentDir,
                    // not a child dir, and therefore puts the '.undeploying' file there
                    File parent = deploymentDir;
                    scannerTasks.add(new UndeployTask(missing, parent));
                }

                if (scannerTasks.size() > 0) {
                    List<ModelNode> updates = new ArrayList<ModelNode>(scannerTasks.size());

                    for (ScannerTask task : scannerTasks) {
                        final ModelNode update = task.getUpdate();
                        if (log.isDebugEnabled()) {
                            log.debugf("Deployment scan of [%s] found update action [%s]", deploymentDir, update);
                        }
                        updates.add(update);
                    }

                    while (!updates.isEmpty()) {
                        ModelNode composite = getCompositeUpdate(updates);
                        final ModelNode results = serverController.execute(OperationBuilder.Factory.create(composite).build());
                        final List<Property> resultList = results.get(RESULT).asPropertyList();
                        final List<ModelNode> toRetry = new ArrayList<ModelNode>();
                        final List<ScannerTask> retryTasks = new ArrayList<ScannerTask>();
                        for (int i = 0; i < resultList.size(); i++) {
                            final ModelNode result = resultList.get(i).getValue();
                            final ScannerTask task = scannerTasks.get(i);
                            final ModelNode outcome = result.get(OUTCOME);
                            if (outcome.isDefined() && SUCCESS.equals(outcome.asString())) {
                                task.handleSuccessResult();
                            } else if (outcome.isDefined() && CANCELLED.equals(outcome.asString())) {
                                toRetry.add(updates.get(i));
                                retryTasks.add(task);
                            } else {
                                task.handleFailureResult(result);
                            }
                        }
                        updates = toRetry;
                        scannerTasks = retryTasks;
                    }
                }
                log.tracef("Scan complete");
            }
        } finally {
            scanLock.unlock();
        }
    }

    /**
     * Scan the given directory for content changes.
     *
     * @param directory             the directory to scan
     * @param events                the event queue;
     * @param registeredDeployments the deployments currently registered with the server
     * @param toRemove              the deployments to be removed, any deployment that needs to stick around should be removed
     */
    private void scanDirectory(final File directory, final List<ScannerTask> events, final Set<String> registeredDeployments, final Set<String> toRemove) {
        final File[] children = directory.listFiles(filter);
        if (children == null) {
            return;
        }

        for (File child : children) {
            final String fileName = child.getName();
            if (fileName.endsWith(DEPLOYED)) {
                final String deploymentName = fileName.substring(0, fileName.length() - DEPLOYED.length());
                toRemove.remove(deploymentName);
                if (!deployed.containsKey(deploymentName)) {
                    if (!child.delete()) {
                        log.warnf("Cannot removed extraneous deployment marker file %s", fileName);
                    }
                }
                if (deployed.get(deploymentName).lastModified != child.lastModified()) {
                    events.add(new RedeployTask(deploymentName, child.lastModified(), directory));
                }
            } else if (fileName.endsWith(DO_DEPLOY)) {
                final String deploymentName = fileName.substring(0, fileName.length() - DO_DEPLOY.length());
                final File deploymentFile = new File(directory, deploymentName);
                if (!deploymentFile.exists()) {
                    log.warnf("Deployment of '%s' requested, but the deployment is not present", deploymentFile);
                    if (!child.delete()) {
                        log.warnf("Cannot removed extraneous deployment marker file %s", fileName);
                    }
                    continue;
                }
                if (registeredDeployments.contains(deploymentName)) {
                    events.add(new ReplaceTask(deploymentName, deploymentFile, child.lastModified()));
                } else {
                    events.add(new DeployTask(deploymentName, deploymentFile, child.lastModified()));
                }
                toRemove.remove(deploymentName);
            } else if (fileName.endsWith(FAILED_DEPLOY)) {
                final String deploymentName = fileName.substring(0, fileName.length() - FAILED_DEPLOY.length());
                toRemove.remove(deploymentName);
            } else if (child.isDirectory()) {
                int idx = fileName.lastIndexOf('.');
                if (idx == -1 || !ARCHIVES.contains(fileName.substring(idx))) {
                    scanDirectory(child, events, registeredDeployments, toRemove);
                }
            }
        }
    }

    private synchronized void startScan() {
        if (scanEnabled) {
            final Runnable runnable = new Runnable() {
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
                scanTask = scheduledExecutor.scheduleWithFixedDelay(runnable, 0, scanInterval, TimeUnit.MILLISECONDS);
            } else {
                scanTask = scheduledExecutor.schedule(runnable, scanInterval, TimeUnit.MILLISECONDS);
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

    private Set<String> getDeploymentNames() throws CancellationException {
        final ModelNode op = Util.getEmptyOperation(READ_CHILDREN_NAMES_OPERATION, new ModelNode());
        op.get(CHILD_TYPE).set(DEPLOYMENT);
        final ModelNode response = serverController.execute(OperationBuilder.Factory.create(op).build());
        final ModelNode result = response.get(RESULT);
        final Set<String> deploymentNames = new HashSet<String>();
        if (result.isDefined()) {
            final List<ModelNode> deploymentNodes = result.asList();
            for (ModelNode node : deploymentNodes) {
                deploymentNames.add(node.asString());
            }
        }
        return deploymentNames;
    }

    private ModelNode getCompositeUpdate(final List<ModelNode> updates) {
        final ModelNode op = Util.getEmptyOperation(COMPOSITE, new ModelNode());
        final ModelNode steps = op.get(STEPS);
        for (ModelNode update : updates) {
            steps.add(update);
        }
        return op;
    }

    private ModelNode getCompositeUpdate(final ModelNode... updates) {
        final ModelNode op = Util.getEmptyOperation(COMPOSITE, new ModelNode());
        final ModelNode steps = op.get(STEPS);
        for (ModelNode update : updates) {
            steps.add(update);
        }
        return op;
    }

    private void safeClose(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void writeFailedMarker(final File deploymentFile, final ModelNode failureDescription) {
        final File failedMarker = new File(deploymentFile.getParent(), deploymentFile.getName() + FAILED_DEPLOY);
        final File deployMarker = new File(deploymentFile.getParent(), deploymentFile.getName() + DO_DEPLOY);
        if (deployMarker.exists() && !deployMarker.delete()) {
            log.warnf("Unable to remove marker file %s", deployMarker);
        }
        final File deployedMarker = new File(deploymentFile.getParent(), deploymentFile.getName() + DEPLOYED);
        if (deployedMarker.exists() && !deployedMarker.delete()) {
            log.warnf("Unable to remove marker file %s", deployedMarker);
        }
        FileOutputStream fos = null;
        try {
            failedMarker.createNewFile();
            fos = new FileOutputStream(failedMarker);
            fos.write(failureDescription.asString().getBytes());
        } catch (IOException io) {
            log.errorf(io, "Caught exception writing deployment failed marker file %s", failedMarker.getAbsolutePath());
        } finally {
            safeClose(fos);
        }
    }

    private abstract class ScannerTask {
        protected final String deploymentName;
        protected final String parent;
        private final String inProgressMarkerSuffix;

        private ScannerTask(final String deploymentName, final File parent, final String inProgressMarkerSuffix) {
            this.deploymentName = deploymentName;
            this.parent = parent.getAbsolutePath();
            this.inProgressMarkerSuffix = inProgressMarkerSuffix;
            File marker = new File(parent, deploymentName + inProgressMarkerSuffix);
            createMarkerFile(marker);
            deleteUndeployedMarker();
        }

        protected abstract ModelNode getUpdate();

        protected abstract void handleSuccessResult();

        protected abstract void handleFailureResult(final ModelNode result);

        protected void createMarkerFile(final File marker) {
            FileOutputStream fos = null;
            try {
                marker.createNewFile();
                fos = new FileOutputStream(marker);
                fos.write(deploymentName.getBytes());
            } catch (IOException io) {
                log.errorf(io, "Caught exception writing deployment marker file %s", marker.getAbsolutePath());
            } finally {
                safeClose(fos);
            }
        }

        protected void deleteUndeployedMarker() {
            final File undeployedMarker = new File(parent, deploymentName + UNDEPLOYED);
            if (undeployedMarker.exists() && !undeployedMarker.delete()) {
                log.warnf("Unable to remove marker file %s", undeployedMarker);
            }
        }

        protected void removeInProgressMarker() {
            File marker = new File(new File(parent), deploymentName + inProgressMarkerSuffix);
            if (marker.exists() && !marker.delete()) {
                log.warnf("Cannot delete deployment progress marker file %s", marker);
            }
        }
    }

    private abstract class ContentAddingTask extends ScannerTask {
        protected final File deploymentFile;
        protected final long doDeployTimestamp;

        protected ContentAddingTask(final String deploymentName, final File deploymentFile, long markerTimestamp) {
            super(deploymentName, deploymentFile.getParentFile(), DEPLOYING);
            this.deploymentFile = deploymentFile;
            this.doDeployTimestamp = markerTimestamp;
        }

        @Override
        protected ModelNode getUpdate() {
            byte[] hash = new byte[0];

            if(deploymentFile.isDirectory()) {
                try {
                    hash = deploymentRepository.addExternalFileReference(deploymentFile);
                } catch(IOException e) {
                    log.error("Failed to add content to deployment repository for [" + deploymentName + "]", e);
                }
                return getUpdatesAfterContent(hash);
            }

            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(deploymentFile);
                hash = deploymentRepository.addDeploymentContent(inputStream);
            } catch (IOException e) {
                log.error("Failed to add content to deployment repository for [" + deploymentName + "]", e);
            } finally {
                safeClose(inputStream);
            }
            return getUpdatesAfterContent(hash);
        }

        protected abstract ModelNode getUpdatesAfterContent(final byte[] hash);

        @Override
        protected void handleSuccessResult() {
            final File doDeployMarker = new File(new File(parent), deploymentFile.getName() + DO_DEPLOY);
            if (doDeployMarker.lastModified() <= doDeployTimestamp) {
                if (!doDeployMarker.delete()) {
                    log.errorf("Failed to delete deployment marker file %s", doDeployMarker.getAbsolutePath());
                }
            }
            // else they copied it in again while deployment was happening; we'll pick it up next scan

            // Remove any previous failure marker
            final File failedMarker = new File(deploymentFile.getParent(), deploymentFile.getName() + FAILED_DEPLOY);
            if (failedMarker.exists() && !failedMarker.delete()) {
                log.warnf("Unable to remove marker file %s", failedMarker);
            }

            // Remove the in-progress marker
            removeInProgressMarker();

            final File deployedMarker = new File(parent, deploymentFile.getName() + DEPLOYED);
            createMarkerFile(deployedMarker);
            if (deployed.containsKey(deploymentName)) {
                deployed.remove(deploymentName);
            }
            deployed.put(deploymentName, new DeploymentMarker(deployedMarker.lastModified()));
        }
    }

    private final class DeployTask extends ContentAddingTask {
        private DeployTask(final String deploymentName, final File deploymentFile, long markerTimestamp) {
            super(deploymentName, deploymentFile, markerTimestamp);
        }

        @Override
        protected ModelNode getUpdatesAfterContent(final byte[] hash) {
            final ModelNode address = new ModelNode().add(DEPLOYMENT, deploymentName);
            final ModelNode addOp = Util.getEmptyOperation(DeploymentAddHandler.OPERATION_NAME, address);
            addOp.get(HASH).set(hash);
            final ModelNode deployOp = Util.getEmptyOperation(DeploymentDeployHandler.OPERATION_NAME, address);
            return getCompositeUpdate(addOp, deployOp);
        }

        @Override
        protected void handleFailureResult(final ModelNode result) {

            // Remove the in-progress marker
            removeInProgressMarker();

            writeFailedMarker(deploymentFile, result.get(FAILURE_DESCRIPTION));
        }
    }

    private final class ReplaceTask extends ContentAddingTask {
        private ReplaceTask(String deploymentName, File deploymentFile, long markerTimestamp) {
            super(deploymentName, deploymentFile, markerTimestamp);
        }

        @Override
        protected ModelNode getUpdatesAfterContent(byte[] hash) {
            final ModelNode replaceOp = Util.getEmptyOperation(DeploymentFullReplaceHandler.OPERATION_NAME, new ModelNode());
            replaceOp.get(NAME).set(deploymentName);
            replaceOp.get(HASH).set(hash);
            return replaceOp;
        }

        @Override
        protected void handleFailureResult(ModelNode result) {

            // Remove the in-progress marker
            removeInProgressMarker();

            writeFailedMarker(deploymentFile, result.get(FAILURE_DESCRIPTION));
        }
    }

    private final class RedeployTask extends ScannerTask {
        private final long markerLastModified;

        private RedeployTask(final String deploymentName, final long markerLastModified, final File parent) {
            super(deploymentName, parent, DEPLOYING);
            this.markerLastModified = markerLastModified;
        }

        @Override
        protected ModelNode getUpdate() {
            final ModelNode address = new ModelNode().add(DEPLOYMENT, deploymentName);
            return Util.getEmptyOperation(DeploymentRedeployHandler.OPERATION_NAME, address);
        }

        @Override
        protected void handleSuccessResult() {

            // Remove the in-progress marker
            removeInProgressMarker();

            deployed.remove(deploymentName);
            deployed.put(deploymentName, new DeploymentMarker(markerLastModified));

        }

        @Override
        protected void handleFailureResult(ModelNode result) {

            // Remove the in-progress marker
            removeInProgressMarker();

            // TODO: Handle Failure
        }
    }

    private final class UndeployTask extends ScannerTask {
        private UndeployTask(final String deploymentName, final File parent) {
            super(deploymentName, parent, UNDEPLOYING);
        }

        @Override
        protected ModelNode getUpdate() {
            final ModelNode address = new ModelNode().add(DEPLOYMENT, deploymentName);
            final ModelNode undeployOp = Util.getEmptyOperation(DeploymentUndeployHandler.OPERATION_NAME, address);
            final ModelNode removeOp = Util.getEmptyOperation(DeploymentRemoveHandler.OPERATION_NAME, address);
            return getCompositeUpdate(undeployOp, removeOp);
        }

        @Override
        protected void handleSuccessResult() {

            // Remove the in-progress marker
            removeInProgressMarker();

            final File deployedMarker = new File(parent, deploymentName + UNDEPLOYED);
            createMarkerFile(deployedMarker);

            deployed.remove(deploymentName);
        }

        @Override
        protected void handleFailureResult(ModelNode result) {

            // Remove the in-progress marker
            removeInProgressMarker();

            // TODO: Handle Failure
        }
    }

    private class DeploymentMarker {
        private long lastModified;

        private DeploymentMarker(final long lastModified) {
            this.lastModified = lastModified;
        }
    }
}
