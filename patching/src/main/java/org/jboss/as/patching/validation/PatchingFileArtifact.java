/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

/**
 * Artifact representing a file.
 *
 * @author Emanuel Muckenhuber
 */
public interface PatchingFileArtifact<P extends PatchingArtifact.ArtifactState, S extends PatchingFileArtifact.FileArtifactState> extends PatchingArtifact<P, S> {

    public interface FileState {

        /**
         * Get the associated file.
         *
         * @return the file
         */
        File getFile();

    }

    static class ConcreteFileArtifact extends AbstractArtifact<DirectoryArtifactState, PatchingFileArtifact.FileArtifactState> implements PatchingFileArtifact<DirectoryArtifactState, FileArtifactState> {

        private final String fileName;

        public ConcreteFileArtifact(String fileName, PatchingArtifact<FileArtifactState, ? extends ArtifactState>... artifacts) {
            super(artifacts);
            this.fileName = fileName;
        }

        @Override
        public boolean process(DirectoryArtifactState parent, PatchingArtifactProcessor processor) {
            final File file = new File(parent.getFile(), fileName);
            final FileArtifactState state = new FileArtifactState(file, this);
            return processor.process(this, state);
        }

    }

    static class ConcreteDirectoryArtifact extends AbstractArtifact<DirectoryArtifactState, PatchingFileArtifact.DirectoryArtifactState> implements PatchingFileArtifact<DirectoryArtifactState, DirectoryArtifactState> {

        private final String fileName;

        public ConcreteDirectoryArtifact(String fileName, PatchingArtifact<DirectoryArtifactState, ? extends ArtifactState>... artifacts) {
            super(artifacts);
            this.fileName = fileName;
        }

        @Override
        public boolean process(DirectoryArtifactState parent, PatchingArtifactProcessor processor) {
            final File file = new File(parent.getFile(), fileName);
            final DirectoryArtifactState state = new DirectoryArtifactState(file, this);
            return processor.process(this, state);
        }
    }

    static class FileArtifactState implements PatchingArtifact.ArtifactState, FileState {

        protected final File file;
        protected final PatchingFileArtifact artifact;

        protected FileArtifactState(File file, PatchingFileArtifact artifact) {
            this.file = file;
            this.artifact = artifact;
        }

        public File getFile() {
            return file;
        }

        @Override
        public boolean isValid(PatchingArtifactValidationContext context) {
            if (file == null) {
                context.getErrorHandler().addError(artifact, this);
                return false;
            } else if (!file.exists()) {
                context.getErrorHandler().addMissing(artifact, this);
                return false;
            }
            return validate0(context);
        }

        protected boolean validate0(PatchingArtifactValidationContext context) {
            if (file.isDirectory()) {
                context.getErrorHandler().addInconsistent(artifact, this);
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            if (file != null) {
                return file.getAbsolutePath();
            } else {
                return artifact.getClass().getName();
            }
        }

    }

    static class DirectoryArtifactState extends FileArtifactState {

        public DirectoryArtifactState(File file, PatchingFileArtifact artifact) {
            super(file, artifact);
        }

        @Override
        protected boolean validate0(PatchingArtifactValidationContext context) {
            if (!file.isDirectory()) {
                context.getErrorHandler().addInconsistent(artifact, this);
                return false;
            }
            return true;
        }

    }

}
