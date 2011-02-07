/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers.deployment;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilterWithAttributes;
import org.jboss.vfs.VisitorAttributes;

/**
 * WS file filter for files with the '.wsdl', or '.xsd' or '.xml' suffix.
 *
 * @author <a href="mailto:dbevenius@jboss.com">Daniel Bevenius</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSVirtualFileFilter implements VirtualFileFilterWithAttributes {
    /** The tree walking attributes. */
    private VisitorAttributes attributes;

    /**
     * Constructor.
     */
    WSVirtualFileFilter() {
        this(VisitorAttributes.RECURSE_LEAVES_ONLY);
    }

    /**
     * Constructor.
     *
     * @param attributes visit attributes
     */
    WSVirtualFileFilter(final VisitorAttributes attributes) {
        this.attributes = attributes;
    }

    /**
     * Gets VisitorAttributes for this instance.
     *
     * @return visitor attributes
     */
    public VisitorAttributes getAttributes() {
        return this.attributes;
    }

    /**
     * Accepts files that end with '.wsdl' or '.xsd' or '.xml'.
     *
     * @param file to analyze
     * @return true if expected file extension, false otherwise
     */
    public boolean accepts(final VirtualFile file) {
        if (file == null) {
            return false;
        }

        final String fileName = file.getName().toLowerCase();
        final boolean hasWsdlSuffix = fileName.endsWith(".wsdl");
        final boolean hasXsdSuffix = fileName.endsWith(".xsd");
        final boolean hasXmlSuffix = fileName.endsWith(".xml");

        return hasWsdlSuffix || hasXsdSuffix || hasXmlSuffix;
    }
}
