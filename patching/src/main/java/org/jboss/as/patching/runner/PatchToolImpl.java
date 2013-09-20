/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.patching.runner;

import static org.jboss.as.patching.IoUtils.safeClose;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.BundledPatch;
import org.jboss.as.patching.metadata.PatchBundleXml;
import org.jboss.as.patching.metadata.PatchMetadataResolver;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.tool.ContentVerificationPolicy;
import org.jboss.as.patching.tool.PatchTool;
import org.jboss.as.patching.tool.PatchingHistory;
import org.jboss.as.patching.tool.PatchingResult;

/**
 * The default patch tool implementation.
 *
 * @author Emanuel Muckenhuber
 */
public class PatchToolImpl implements PatchTool {

    private final InstallationManager manager;
    private final InstallationManager.ModificationCompletionCallback callback;
    private final IdentityPatchRunner runner;

    public PatchToolImpl(final InstallationManager manager) {
        this.manager = manager;
        this.runner = new IdentityPatchRunner(manager.getInstalledImage());
        this.callback = runner;
    }

    public PatchToolImpl(final InstallationManager manager, final InstallationManager.ModificationCompletionCallback callback) {
        this.manager = manager;
        this.runner = new IdentityPatchRunner(manager.getInstalledImage());
        this.callback = callback;
    }

    @Override
    public PatchInfo getPatchInfo() {
        try {
            final Identity identity = manager.getIdentity();
            final PatchableTarget.TargetInfo info = manager.getIdentity().loadTargetInfo();
            return new PatchInfo() {
                @Override
                public String getVersion() {
                    return identity.getVersion();
                }

                @Override
                public String getCumulativePatchID() {
                    return info.getCumulativePatchID();
                }

                @Override
                public List<String> getPatchIDs() {
                    return info.getPatchIDs();
                }
            };
        } catch (IOException e) {
            // why throw a rethrowException(e) ?
            throw new RuntimeException(e);
        }
    }

    @Override
    public PatchingHistory getPatchingHistory() {
        return PatchingHistory.Factory.getHistory(manager);
    }

    @Override
    public PatchingResult applyPatch(final File file, final ContentVerificationPolicy contentPolicy) throws PatchingException {
        try {
            if (file.isDirectory()) {
                final File patchXml = new File(file, PatchXml.PATCH_XML);
                if (patchXml.exists()) {
                    // Shortcut exploded patches
                    return execute(file, contentPolicy);
                }
            }
            final InputStream is = new FileInputStream(file);
            try {
                return applyPatch(is, contentPolicy);
            } finally {
                if(is != null) try {
                    is.close();
                } catch (IOException e) {
                    PatchLogger.ROOT_LOGGER.debugf(e, "failed to close input stream");
                }
            }
        } catch (Exception e) {
            throw rethrowException(e);
        }
    }

    @Override
    public PatchingResult applyPatch(final URL url, final ContentVerificationPolicy contentPolicy) throws PatchingException {
        try {
            final InputStream is = url.openStream();
            try {
                return applyPatch(is, contentPolicy);
            } finally {
                if(is != null) try {
                    is.close();
                } catch (IOException e) {
                    PatchLogger.ROOT_LOGGER.debugf(e, "failed to close input stream");
                }
            }
        } catch (IOException e) {
            throw new PatchingException(e);
        }
    }

    @Override
    public PatchingResult applyPatch(final InputStream is, final ContentVerificationPolicy contentPolicy) throws PatchingException {
        return applyPatch(null, is, contentPolicy);
    }

    private PatchingResult applyPatch(final File parentWorkDir, final InputStream is, final ContentVerificationPolicy contentPolicy) throws PatchingException {
        File workDir = null;
        try {
            // Create a working dir
            workDir = parentWorkDir == null ? IdentityPatchRunner.createTempDir() : IdentityPatchRunner.createTempDir(parentWorkDir);

            // Save the content
            final File cachedContent = new File(workDir, "content");
            IoUtils.copy(is, cachedContent);
            // Unpack to the work dir
            ZipUtils.unzip(cachedContent, workDir);

            // Execute
            return execute(workDir, contentPolicy);
        } catch (Exception e) {
            throw rethrowException(e);
        } finally {
            if (workDir != null && !IoUtils.recursiveDelete(workDir)) {
                PatchLogger.ROOT_LOGGER.cannotDeleteFile(workDir.getAbsolutePath());
            }
        }
    }

    @Override
    public PatchingResult rollback(final String patchId, final ContentVerificationPolicy contentPolicy,
                                   final boolean rollbackTo, final boolean resetConfiguration) throws PatchingException {
        // Rollback the patch
        final InstallationManager.InstallationModification modification = manager.modifyInstallation(runner);
        try {
            return runner.rollbackPatch(patchId, contentPolicy, rollbackTo, resetConfiguration, modification);
        } catch (Exception e) {
            modification.cancel();
            throw rethrowException(e);
        }
    }

    @Override
    public PatchingResult rollbackLast(final ContentVerificationPolicy contentPolicy, final boolean resetConfiguration) throws PatchingException {
        // Rollback the patch
        final InstallationManager.InstallationModification modification = manager.modifyInstallation(runner);
        try {
            return runner.rollbackLast(contentPolicy, resetConfiguration, modification);
        } catch (Exception e) {
            modification.cancel();
            throw rethrowException(e);
        }
    }

    protected PatchingResult execute(final File workDir, final ContentVerificationPolicy contentPolicy) throws PatchingException, IOException, XMLStreamException {

        final File patchBundleXml = new File(workDir, PatchBundleXml.MULTI_PATCH_XML);
        if (patchBundleXml.exists()) {
            final InputStream patchIs = new FileInputStream(patchBundleXml);
            try {
                // Handle multi patch installs
                final BundledPatch bundledPatch = PatchBundleXml.parse(patchIs);
                return applyPatchBundle(workDir, bundledPatch, contentPolicy);
            } finally {
                safeClose(patchIs);
            }
        } else {
            // Parse the xml
            final File patchXml = new File(workDir, PatchXml.PATCH_XML);
            final PatchContentProvider contentProvider = PatchContentProvider.DefaultContentProvider.create(workDir);
            final InputStream patchIS = new FileInputStream(patchXml);
            final PatchMetadataResolver patchResolver;
            try {
                patchResolver = PatchXml.parse(patchIS);
                patchIS.close();
            } finally {
                safeClose(patchIS);
            }
            return apply(patchResolver, contentProvider, contentPolicy);
        }
    }

    protected PatchingResult apply(final PatchMetadataResolver patchResolver, final PatchContentProvider contentProvider, final ContentVerificationPolicy contentPolicy) throws PatchingException {
        // Apply the patch
        final InstallationManager.InstallationModification modification = manager.modifyInstallation(callback);
        try {
            return runner.applyPatch(patchResolver, contentProvider, contentPolicy, modification);
        } catch (Exception e) {
            modification.cancel();
            throw rethrowException(e);
        }
    }

    protected PatchingResult applyPatchBundle(final File workDir, final BundledPatch bundledPatch, final ContentVerificationPolicy contentPolicy) throws PatchingException, IOException {
        PatchingResult result = null;
        final List<BundledPatch.BundledPatchEntry> results = new ArrayList<BundledPatch.BundledPatchEntry>();
        final Iterator<BundledPatch.BundledPatchEntry> iterator = bundledPatch.getPatches().iterator();
        while (iterator.hasNext()) {
            final BundledPatch.BundledPatchEntry entry = iterator.next();
            final File patch = new File(workDir, entry.getPatchPath());
            final FileInputStream is = new FileInputStream(patch);
            try {
                result = applyPatch(workDir, is, contentPolicy);
            } catch (PatchingException e) {
                // Undo the changes included as part of this patch
                for (BundledPatch.BundledPatchEntry committed : results) {
                    try {
                        rollback(committed.getPatchId(), contentPolicy, false, false).commit();
                    } catch (PatchingException oe) {
                        PatchLogger.ROOT_LOGGER.debugf(oe, "failed to rollback patch '%s'", committed.getPatchId());
                    }
                }
                throw e;
            } finally {
                safeClose(is);
            }
            if (iterator.hasNext()) {
                result.commit();
                results.add(0, entry);
            }
        }
        if (result == null) {
            throw new PatchingException();
        }
        return new WrappedMultiInstallPatch(result, contentPolicy, results);
    }

    static PatchingException rethrowException(final Exception e) {
        if (e instanceof PatchingException) {
            return (PatchingException) e;
        } else {
            return new PatchingException(e);
        }
    }

    class WrappedMultiInstallPatch implements PatchingResult {

        private final PatchingResult last;
        private final ContentVerificationPolicy policy;
        private final List<BundledPatch.BundledPatchEntry> committed;

        WrappedMultiInstallPatch(PatchingResult last, ContentVerificationPolicy policy, List<BundledPatch.BundledPatchEntry> committed) {
            this.last = last;
            this.policy = policy;
            this.committed = committed;
        }

        @Override
        public String getPatchId() {
            return last.getPatchId();
        }

        @Override
        public PatchInfo getPatchInfo() {
            return last.getPatchInfo();
        }

        @Override
        public void commit() {
            last.commit();
        }

        @Override
        public void rollback() {
            last.rollback(); // Rollback the last
            for (final BundledPatch.BundledPatchEntry entry : committed) {
                try {
                    PatchToolImpl.this.rollback(entry.getPatchId(), policy, false, false).commit();
                } catch (Exception e) {
                    PatchLogger.ROOT_LOGGER.debugf(e, "failed to rollback patch '%s'", entry.getPatchId());
                }
            }
        }
    }

}
