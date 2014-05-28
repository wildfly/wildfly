/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.patching.runner;

import java.io.File;
import java.util.Objects;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class FailedFileRenaming {

    private final String sourceFile;
    private final String targetFile;
    private final String patchId;

    public FailedFileRenaming(final File sourceFile, final File targetFile, final String applyPatchId) {
        this.sourceFile = sourceFile.getAbsolutePath();
        this.targetFile = targetFile.getAbsolutePath();
        this.patchId = applyPatchId;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public String getPatchId() {
        return patchId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.sourceFile);
        hash = 29 * hash + Objects.hashCode(this.targetFile);
        hash = 29 * hash + Objects.hashCode(this.patchId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FailedFileRenaming other = (FailedFileRenaming) obj;
        if (!Objects.equals(this.sourceFile, other.sourceFile)) {
            return false;
        }
        if (!Objects.equals(this.targetFile, other.targetFile)) {
            return false;
        }
        if (!Objects.equals(this.patchId, other.patchId)) {
            return false;
        }
        return true;
    }

}
