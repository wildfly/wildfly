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

package org.jboss.as.threads;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractExecutorElement<T extends AbstractExecutorElement<T>> extends AbstractModelElement<T> {

    private static final long serialVersionUID = -2409073407325398348L;

    private final String name;
    private final Map<String, String> properties = new HashMap<String, String>();

    private String threadFactory;
    private ScaledCount maxThreads;
    private TimeSpec keepaliveTime;

    protected AbstractExecutorElement(final String name) {
        this.name = name;
    }

    protected static void writeTimeSpecElement(final XMLExtendedStreamWriter writer, final TimeSpec timeSpec, final String localName) throws XMLStreamException {
        writer.writeEmptyElement(localName);
        writer.writeAttribute("time", Long.toString(timeSpec.getDuration()));
        writer.writeAttribute("unit", timeSpec.getUnit().toString().toLowerCase(Locale.ENGLISH));
    }

    protected static void writeScaledCountElement(final XMLExtendedStreamWriter writer, final ScaledCount scaledCount, final String localName) throws XMLStreamException {
        if (scaledCount == null) {
            return;
        }
        writer.writeEmptyElement(localName);
        final BigDecimal count = scaledCount.getCount();
        if (count.compareTo(BigDecimal.ZERO) > 0) writer.writeAttribute("count", count.toPlainString());
        final BigDecimal perCpu = scaledCount.getPerCpu();
        if (perCpu.compareTo(BigDecimal.ZERO) > 0) writer.writeAttribute("per-cpu", perCpu.toPlainString());
    }

    public String getThreadFactory() {
        return threadFactory;
    }

    void setThreadFactory(final String threadFactory) {
        this.threadFactory = threadFactory;
    }

    public ScaledCount getMaxThreads() {
        return maxThreads;
    }

    void setMaxThreads(final ScaledCount maxThreads) {
        this.maxThreads = maxThreads;
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    Map<String, String> getProperties() {
        return properties;
    }

    public TimeSpec getKeepaliveTime() {
        return keepaliveTime;
    }

    void setKeepaliveTime(final TimeSpec keepaliveTime) {
        this.keepaliveTime = keepaliveTime;
    }

    public final String getName() {
        return name;
    }

    public final void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        writeAttributes(streamWriter);
        writeElements(streamWriter);
        streamWriter.writeEndElement();
    }

    protected void writeAttributes(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute("name", getName());
    }

    protected void writeElements(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        final ScaledCount maxThreads = getMaxThreads();
        if (maxThreads != null) writeScaledCountElement(streamWriter, maxThreads, "max-threads");
        final TimeSpec keepaliveTime = getKeepaliveTime();
        if (keepaliveTime != null) writeTimeSpecElement(streamWriter, keepaliveTime, "keepalive-time");
        final String threadFactory = getThreadFactory();
        if (threadFactory != null) {
            streamWriter.writeEmptyElement("thread-factory");
            streamWriter.writeAttribute("name", threadFactory);
        }
        if (! properties.isEmpty()) {
            streamWriter.writeStartElement("properties");
            for (String name : properties.keySet()) {
                streamWriter.writeEmptyElement("property");
                streamWriter.writeAttribute("name", name);
                streamWriter.writeAttribute("value", properties.get(name));
            }
            streamWriter.writeEndElement();
        }
    }

    abstract AbstractThreadsSubsystemUpdate<Void> getAdd();
}
