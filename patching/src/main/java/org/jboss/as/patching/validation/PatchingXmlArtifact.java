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

import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchMetadataResolver;
import org.jboss.as.patching.metadata.PatchXml;

/**
 * Artifact representing a xml file.
 *
 * @author Alexey Loubyansky
 * @author Emanuel Muckenhuber
 */
class PatchingXmlArtifact<E extends Patch> extends AbstractArtifact<PatchingFileArtifact.FileArtifactState, PatchingXmlArtifact.XmlArtifactState<E>> {

    PatchingXmlArtifact(PatchingArtifact<XmlArtifactState<E>, ? extends ArtifactState>... artifacts) {
        super(artifacts);
    }

    @Override
    public boolean process(PatchingFileArtifact.FileArtifactState parent, PatchingArtifactProcessor processor) {
        final File xmlFile = parent.getFile();
        final XmlArtifactState<E> state = new XmlArtifactState<E>(xmlFile, this);
        return processor.process(this, state);
    }

    protected E resolveMetaData(PatchMetadataResolver resolver) throws PatchingException {
        throw new IllegalStateException(); // this gets overriden by the actual artifacts used
    }

    static class XmlArtifactState<E extends Patch> implements PatchingArtifact.ArtifactState {

        private final File xmlFile;
        private final PatchingXmlArtifact<E> artifact;
        private E patch;

        XmlArtifactState(File xmlFile, PatchingXmlArtifact<E> artifact) {
            this.xmlFile = xmlFile;
            this.artifact = artifact;
        }

        public E getPatch() {
            return patch;
        }

        @Override
        public boolean isValid(PatchingArtifactValidationContext context) {
            if (patch != null) {
                return true;
            }
            try {
                final PatchMetadataResolver resolver = PatchXml.parse(xmlFile, context.getCurrentPatchIdentity());
                patch = artifact.resolveMetaData(resolver);
                return true;
            } catch (Exception e) {
                context.getErrorHandler().addError(artifact, this);
            }
            return false;
        }

        @Override
        public String toString() {
            return xmlFile.getAbsolutePath();
        }
    }

}
