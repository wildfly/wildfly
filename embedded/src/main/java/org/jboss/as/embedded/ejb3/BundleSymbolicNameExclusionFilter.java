/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.embedded.ejb3;

import static org.jboss.as.embedded.EmbeddedLogger.ROOT_LOGGER;
import static org.jboss.as.embedded.EmbeddedMessages.MESSAGES;

import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ExclusionFilter} implementation which
 * will block OSGi bundles with the header "Bundle-SymbolicName"
 * if the value matches one in a configurable set.
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 * @version $Revision: $
 */
public class BundleSymbolicNameExclusionFilter implements ExclusionFilter {

    //-------------------------------------------------------------------------------------||
    // Class Members ----------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    /**
     * Key of the bundle symbolic name header
     */
    private static final String HEADER_BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";

    /**
     * Location of the manifest file under the root
     */
    private static final String NAME_MANIFEST = "META-INF/MANIFEST.MF";

    //-------------------------------------------------------------------------------------||
    // Instance Members -------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    /**
     * Patterns to exclude if present in value of the bundle symbolic name header
     */
    private final Set<String> exclusionValues;

    //-------------------------------------------------------------------------------------||
    // Constructor ------------------------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    /**
     * Creates a new instance configured to the specified exclusion values
     *
     * @param exclusionValues Patterns to exclude if present in the bundle symbolic name header
     * @throws IllegalArgumentException If no exclusions are specified
     */
    public BundleSymbolicNameExclusionFilter(final String... exclusionValues) throws IllegalArgumentException {
        // Precondition check
        if (exclusionValues == null || exclusionValues.length == 0) {
            throw MESSAGES.exclusionValuesRequired();
        }

        // Defensive copy on set and make immutable
        final Set<String> excludeSet = new HashSet<String>();
        for (final String exclusionValue : exclusionValues) {
            excludeSet.add(exclusionValue);
        }
        this.exclusionValues = Collections.unmodifiableSet(excludeSet);

    }

    //-------------------------------------------------------------------------------------||
    // Required Implementations -----------------------------------------------------------||
    //-------------------------------------------------------------------------------------||

    /**
     * {@inheritDoc}
     *
     * @see ExclusionFilter#exclude(org.jboss.vfs.VirtualFile)
     */
    @Override
    public boolean exclude(final VirtualFile file) {
        // Precondition checks
        if (file == null) {
            throw MESSAGES.nullVar("file");
        }

        // If this exists, first of all
        if (!file.exists()) {
            return false;
        }

        // Get the Manifest
        final VirtualFile manifest = file.getChild(NAME_MANIFEST);
        if (!manifest.exists()) {
            return false;
        }

        // Inspect the manifest contents
        final LineNumberReader reader;
        try {
            reader = new LineNumberReader(new InputStreamReader(manifest.openStream()));
            String line;
            // Read each line
            while ((line = reader.readLine()) != null) {
                // If this is the bundle symbolic name header
                final String header = HEADER_BUNDLE_SYMBOLIC_NAME;
                if (line.contains(header)) {
                    // Check if it also contains a matching value
                    for (final String exclusionValue : this.exclusionValues) {
                        if (line.contains(exclusionValue)) {
                            if (ROOT_LOGGER.isTraceEnabled()) {
                                ROOT_LOGGER.tracef("Configured exclusion value \"%s\" encountered in manifest header \"%s\"; skipping %s",
                                        exclusionValue, header, file);
                            }
                            // Skip
                            return true;
                        }
                    }
                }
            }
        } catch (final IOException ioe) {
            throw MESSAGES.cannotReadContent(ioe, file);
        }

        // No conditions met
        return false;
    }

}
