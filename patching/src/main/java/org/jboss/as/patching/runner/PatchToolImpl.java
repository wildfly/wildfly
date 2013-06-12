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
import java.util.List;

import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.ZipUtils;
import org.jboss.as.patching.installation.Identity;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.InstalledImage;
import org.jboss.as.patching.installation.PatchableTarget;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.tool.PatchTool;

/**
 * The default patch tool implementation.
 *
 * @author Emanuel Muckenhuber
 */
public class PatchToolImpl implements PatchTool {

    private final InstalledImage image;
    private final InstallationManager manager;
    private final IdentityPatchRunner runner;

    public PatchToolImpl(final InstallationManager manager) {
        this.manager = manager;
        try {
            this.image = manager.getIdentity().loadTargetInfo().getDirectoryStructure().getInstalledImage();
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO manager.getInstalledImage()
        }
        this.runner = new IdentityPatchRunner(image);
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
                public String getCumulativeID() {
                    return info.getCumulativeID();
                }

                @Override
                public List<String> getPatchIDs() {
                    return info.getPatchIDs();
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        } catch (PatchingException e) {
            throw e;
        } catch (Exception e) {
            throw new PatchingException(e);
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
        File workDir = null;
        try {
            // Create a working dir
            workDir = IdentityPatchRunner.createTempDir();

            // Save the content
            final File cachedContent = new File(workDir, "content");
            IoUtils.copy(is, cachedContent);
            // Unpack to the work dir
            ZipUtils.unzip(cachedContent, workDir);

            // Execute
            return execute(workDir, contentPolicy);
        } catch (PatchingException e) {
            throw e;
        } catch (Exception e) {
            throw new PatchingException(e);
        } finally {
            if (workDir != null && !IoUtils.recursiveDelete(workDir)) {
                PatchLogger.ROOT_LOGGER.debugf("failed to remove work directory (%s)", workDir);
            }
        }
    }

    @Override
    public PatchingResult rollback(final String patchId, final ContentVerificationPolicy contentPolicy,
                                   final boolean rollbackTo, final boolean restoreConfiguration) throws PatchingException {
        // Rollback the patch
        final InstallationManager.InstallationModification modification = manager.modifyInstallation(runner);
        return runner.rollbackPatch(patchId, contentPolicy, rollbackTo, restoreConfiguration, modification);
    }

    protected PatchingResult execute(final File workDir, final ContentVerificationPolicy contentPolicy) throws PatchingException, IOException, XMLStreamException {

        // Parse the xml
        final PatchContentProvider contentProvider = PatchContentProvider.DefaultContentProvider.create(workDir);
        final File patchXml = new File(workDir, PatchXml.PATCH_XML);
        final InputStream patchIS = new FileInputStream(patchXml);
        final Patch patch;
        try {
            patch = PatchXml.parse(patchIS);
            patchIS.close();
        } finally {
            safeClose(patchIS);
        }

        // Apply the patch
        final InstallationManager.InstallationModification modification = manager.modifyInstallation(runner);
        return runner.applyPatch(patch, contentProvider, contentPolicy, modification);
    }

}
