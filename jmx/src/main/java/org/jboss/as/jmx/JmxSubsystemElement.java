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

package org.jboss.as.jmx;

import java.util.Collection;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import org.jboss.as.model.AbstractModelUpdate;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * JMX Subsystem element implementation.
 *
 * @author John Bailey
 */
class JmxSubsystemElement extends AbstractSubsystemElement<JmxSubsystemElement> {
    private static final Logger log = Logger.getLogger("org.jboss.as.jmx");

    /**
     * Construct a new instance by parsing.
     *
     * @param reader The XML reader instance to use for parsing
     * @throws XMLStreamException If any parsing errors occur
     */
    public JmxSubsystemElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        requireNoContent(reader);
    }

    /**
     * Activate the JMX subsystem.
     *
     * @param context the service activation context
     */
    public void activate(final ServiceActivatorContext context) {
        log.info("Activating the JMX Subsystem");
        final BatchBuilder batchBuilder = context.getBatchBuilder();
        batchBuilder.addService(MBeanServerService.MBEAN_SERVER_SERVICE_NAME, new MBeanServerService())
            .setInitialMode(ServiceController.Mode.IMMEDIATE);
    }

    /** {@inheritDoc} */
    public long elementHash() {
        return 42;
    }

    /** {@inheritDoc} */
    protected void appendDifference(final Collection<AbstractModelUpdate<JmxSubsystemElement>> target, final JmxSubsystemElement other) {
    }

    /** {@inheritDoc} */
    protected Class<JmxSubsystemElement> getElementClass() {
        return JmxSubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }
}
