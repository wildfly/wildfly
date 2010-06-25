/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.domain;

import java.util.Collection;
import java.util.Collections;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class DomainSubsystem extends AbstractDomainElement<DomainSubsystem> {

    private static final long serialVersionUID = -90177370272205647L;

    private final String name;
    private final String moduleIdentifier;
    private volatile boolean enabled;

    /**
     * Construct a new instance.
     *
     * @param name the subsystem name
     * @param moduleIdentifier the module identifier of the subsystem, if any
     */
    public DomainSubsystem(final String name, final String moduleIdentifier) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
        this.moduleIdentifier = moduleIdentifier;
    }

    /** {@inheritDoc} */
    public long elementHash() {
        final String moduleIdentifier = this.moduleIdentifier;
        long hc = name.hashCode() & 0xFFFFFFFFL << 32L | (moduleIdentifier == null ? 0 : moduleIdentifier.hashCode() & 0xFFFFFFFF);
        if (enabled) hc ++;
        return hc;
    }

    /** {@inheritDoc} */
    public Collection<? extends AbstractDomainUpdate<?>> getDifference(final DomainSubsystem other) {
        assert isSameElement(other);
        if (enabled == other.enabled) {
            return Collections.emptySet();
        } else {
            // TODO - enable/disable mgmt op
            return Collections.singleton(null);
        }
    }

    /** {@inheritDoc} */
    public boolean isSameElement(final DomainSubsystem other) {
        return name.equals(other.name) && (moduleIdentifier == null ? other.moduleIdentifier == null : moduleIdentifier.equals(other.moduleIdentifier));
    }

    /**
     * Determine whether this subsystem is enabled.
     *
     * @return {@code true} if the subsystem is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void writeContent(final XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEmptyElement(Domain.NAMESPACE, "subsystem");
        streamWriter.writeAttribute("name", name);
        if (moduleIdentifier != null) streamWriter.writeAttribute("module", moduleIdentifier);
        if (enabled) streamWriter.writeAttribute("enabled", "true");
    }
}
