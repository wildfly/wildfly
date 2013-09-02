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

package org.jboss.as.patching.validation;

import java.io.File;

import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.metadata.PatchXml;

/**
 * @author Alexey Loubyansky
 *
 */
public class XmlFileState implements Artifact.State {

    protected final File file;
    protected PatchBuilder patchBuilder;
    protected Patch patch;

    protected XmlFileState(File file) {
        if(file == null) {
            throw new IllegalArgumentException("File is null");
        }
        this.file = file;
    }

    @Override
    public void validate(Context ctx) {
        if(!file.exists()) {
            ctx.getErrorHandler().error("File doesn't exist: " + file.getAbsolutePath());
        } else {
            try {
                patchBuilder = (PatchBuilder) PatchXml.parse(file);
            } catch (Exception e) {
                ctx.getErrorHandler().error("Failed to parse file: " + file.getAbsolutePath(), e);
            }
        }
    }

    public File getFile() {
        return file;
    }

    public Patch getPatch() {
        if(patch == null) {
            patch = patchBuilder.build();
        }
        return patch;
    }
}
