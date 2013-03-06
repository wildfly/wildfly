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

package org.jboss.as.patching.tool;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchLogger;
import org.jboss.as.patching.runner.ContentVerificationPolicy;
import org.jboss.as.patching.runner.PatchingException;
import org.jboss.as.patching.runner.PatchingResult;
import org.jboss.as.patching.runner.PatchingTaskRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * An interactive offline patch tool.
 *
 * @author Emanuel Muckenhuber
 */
class LocalPatchTool implements PatchTool {


    private final PatchInfo patchInfo;
    private final PatchingTaskRunner runner;
    LocalPatchTool(PatchInfo info, DirectoryStructure structure) {
        this.patchInfo = info;
        this.runner = new PatchingTaskRunner(info, structure);

    }

    @Override
    public PatchInfo getPatchInfo() {
        return patchInfo;
    }

    @Override
    public PatchingResult applyPatch(File file, ContentVerificationPolicy policy) throws PatchingException {
        try {
            final InputStream is = new FileInputStream(file);
            try {
                return applyPatch(is, policy);
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
    public PatchingResult applyPatch(URL url, ContentVerificationPolicy policy) throws PatchingException {
        try {
            final InputStream is = url.openStream();
            try {
                return applyPatch(is, policy);
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
    public PatchingResult applyPatch(InputStream is, ContentVerificationPolicy contentPolicy) throws PatchingException {
        return runner.executeDirect(is, contentPolicy);
    }

    @Override
    public PatchingResult rollback(String patchId, ContentVerificationPolicy policy, boolean rollbackTo, boolean restoreConfiguration) throws PatchingException {
        return runner.rollback(patchId, policy, rollbackTo, restoreConfiguration);
    }

}
