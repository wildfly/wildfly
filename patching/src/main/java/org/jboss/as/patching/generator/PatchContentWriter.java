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

package org.jboss.as.patching.generator;

import static org.jboss.as.patching.generator.PatchGenerator.processingError;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.IoUtils;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentModification;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModificationType;
import org.jboss.as.patching.metadata.ModuleItem;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchElement;
import org.jboss.as.patching.metadata.PatchXml;
import org.jboss.as.patching.runner.PatchContentLoader;

/**
 * The patch content writer.
 * <p/>
 * Structure:
 * - patch.xml
 * - identity-patch-id
 *    - misc
 * - element-patch-id-1
 *    - modules
 *    - bundles
 * - other-element-patch-id-2
 *    - modules
 *    - bundles
 *
 * @author Emanuel Muckenhuber
 */
abstract class PatchContentWriter {

    abstract File getSourceFile(final ContentItem item) throws IOException;

    abstract File getTargetFile(final ContentItem item) throws IOException;

    /**
     * Recursively copy modification items.
     *
     * @param modifications the modifications
     * @throws IOException
     */
    protected void copyItems(final Collection<ContentModification> modifications) throws IOException {
        for (final ContentModification modification : modifications) {
            if (modification.getType() == ModificationType.REMOVE) {
                // Skip removals
                continue;
            }
            final ContentItem item = modification.getItem();
            final File source = getSourceFile(item);
            final File target = getTargetFile(item);
            if (!source.exists()) {
                throw processingError("source item does not exist %s", source.getAbsolutePath());
            }
            IoUtils.copyFile(source, target);
        }
    }

    static void process(final File targetRoot, final File distributionRoot, final Patch patch) throws IOException, XMLStreamException {
        try {
            targetRoot.mkdirs();
            // Write the patch xml
            final File patchXml = new File(targetRoot, PatchXml.PATCH_XML);
            final FileOutputStream os = new FileOutputStream(patchXml);
            try {
                PatchXml.marshal(os, patch);
            } finally {
                IoUtils.safeClose(os);
            }
            // Copy
            internalProcess(targetRoot, distributionRoot, patch);

        } finally {

        }
    }

    /**
     * Process the patch.
     *
     * @param targetRoot       the target root
     * @param distributionRoot the distribution root
     * @param patch            the patch
     * @throws IOException
     * @throws XMLStreamException
     */
    static void internalProcess(final File targetRoot, final File distributionRoot, final Patch patch) throws IOException, XMLStreamException {

        // TODO get from distribution structure ...
        final File bundles = new File(distributionRoot, "bundles");
        final File modules = new File(distributionRoot, "modules");

        // Copy content for all elements
        for (final PatchElement element : patch.getElements()) {

            final File elementRoot = new File(targetRoot, element.getId());
            final String base = element.getProvider().isAddOn() ? Constants.DEFAULT_ADD_ONS_PATH : Constants.DEFAULT_LAYERS_PATH;

            final PatchContentLoader elementLoader = PatchContentLoader.create(elementRoot);
            final PatchContentWriter elementWriter = new PatchContentWriter() {
                @Override
                File getSourceFile(ContentItem item) throws IOException {
                    if (item.getContentType() == ContentType.BUNDLE) {
                        final File root = new File(bundles, base);
                        final File layer = new File(root, element.getProvider().getName());
                        return PatchContentLoader.getModulePath(layer, (ModuleItem) item);
                    } else if (item.getContentType() == ContentType.MODULE) {
                        final File root = new File(modules, base);
                        final File layer = new File(root, element.getProvider().getName());
                        return PatchContentLoader.getModulePath(layer, (ModuleItem) item);
                    }
                    throw processingError("invalid content item for patch-element %s", item);
                }

                @Override
                File getTargetFile(ContentItem item) throws IOException {
                    return elementLoader.getFile(item);
                }
            };
            // Copy
            elementWriter.copyItems(element.getModifications());
        }

        // Copy misc items for distribution
        final File patchRoot = new File(targetRoot, patch.getPatchId());
        final PatchContentLoader targetLoader = PatchContentLoader.create(patchRoot);
        final PatchContentWriter writer = new PatchContentWriter() {
            @Override
            File getSourceFile(ContentItem item) throws IOException {
                if (item.getContentType() == ContentType.MISC) {
                    return PatchContentLoader.getMiscPath(distributionRoot, (MiscContentItem) item);
                }
                throw processingError("invalid content item for identity %s", item);
            }

            @Override
            File getTargetFile(ContentItem item) throws IOException {
                return targetLoader.getFile(item);
            }
        };
        // Copy root
        writer.copyItems(patch.getModifications());

    }


}
