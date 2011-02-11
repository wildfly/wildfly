/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.module;

import java.net.URI;

/**
 * An extension list entry.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ExtensionListEntry {
    private final String title;
    private final String name;
    private final String specificationVersion;
    private final String implementationVersion;
    private final String implementationVendorId;
    private final URI implementationUrl;

    /**
     * Construct a new instance.
     *
     * @param title the name of the value of the {@code Extension-List} attribute for this item
     * @param name the value of the {@code &lt;extension&gt;-Extension-Name} attribute
     * @param specificationVersion the value of the {@code &lt;extension&gt;-Specification-Version} attribute
     * @param implementationVersion the value of the {@code &lt;extension&gt;-Implementation-Version} attribute
     * @param implementationVendorId the value of the {@code &lt;extension&gt;-Implementation-Vendor-Id} attribute
     * @param implementationUrl the value of the {@code &lt;extension&gt;-Implementation-URL} attribute
     */
    public ExtensionListEntry(final String title, final String name, final String specificationVersion, final String implementationVersion, final String implementationVendorId, final URI implementationUrl) {
        this.title = title;
        this.name = name;
        this.specificationVersion = specificationVersion;
        this.implementationVersion = implementationVersion;
        this.implementationVendorId = implementationVendorId;
        this.implementationUrl = implementationUrl;
    }

    /**
     * Get the extension list title (from the {@code Extension-List} attribute) for this entry.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get the extension name.
     *
     * @return the extension name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the specification version.
     *
     * @return the specification version
     */
    public String getSpecificationVersion() {
        return specificationVersion;
    }

    /**
     * Get the implementation version.
     *
     * @return the implementation version
     */
    public String getImplementationVersion() {
        return implementationVersion;
    }

    /**
     * Get the implementation vendor ID.
     *
     * @return the implementation vendor ID
     */
    public String getImplementationVendorId() {
        return implementationVendorId;
    }

    /**
     * Get the implementation URL.
     *
     * @return the implementation URL
     */
    public URI getImplementationUrl() {
        return implementationUrl;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExtensionListEntry [");
        if (implementationUrl != null)
            builder.append("implementationUrl=").append(implementationUrl).append(", ");
        if (implementationVendorId != null)
            builder.append("implementationVendorId=").append(implementationVendorId).append(", ");
        if (implementationVersion != null)
            builder.append("implementationVersion=").append(implementationVersion).append(", ");
        if (name != null)
            builder.append("name=").append(name).append(", ");
        if (specificationVersion != null)
            builder.append("specificationVersion=").append(specificationVersion).append(", ");
        if (title != null)
            builder.append("title=").append(title);
        builder.append("]");
        return builder.toString();
    }

}
