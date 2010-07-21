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

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractExecutorElement<T extends AbstractExecutorElement<T>> extends AbstractModelElement<T> implements ServiceActivator {

    private static final long serialVersionUID = -2409073407325398348L;

    /**
     * The service name under which thread-related services are registered.
     */
    public static ServiceName JBOSS_THREAD = ServiceName.JBOSS.append("thread");
    /**
     * The service name under which thread factories are registered.
     */
    public static ServiceName JBOSS_THREAD_FACTORY = JBOSS_THREAD.append("factory");
    /**
     * The service name under which executors (thread pools) are registered.
     */
    public static ServiceName JBOSS_THREAD_EXECUTOR = JBOSS_THREAD.append("executor");
    /**
     * The service name under which scheduled executors are registered.
     */
    public static ServiceName JBOSS_THREAD_SCHEDULED_EXECUTOR = JBOSS_THREAD.append("scheduled-executor");

    private final String name;

    private String threadFactory;
    private ScaledCount maxThreads;
    private TimeSpec keepaliveTime;

    private PropertiesElement properties;

    protected AbstractExecutorElement(final Location location, final String name) {
        super(location);
        this.name = name;
    }

    protected AbstractExecutorElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
        name = reader.getAttributeValue(null, "name");
        if (name == null) {
            throw missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
    }

    protected static TimeSpec readTimeSpecElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        TimeUnit unit = null;
        long qty = -1L;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                case TIME: {
                    qty = reader.getLongAttributeValue(i);
                    break;
                }
                case UNIT: {
                    unit = reader.getAttributeValue(i, TimeUnit.class);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (qty == -1L) throw missingRequired(reader, Collections.singleton(Attribute.TIME));
        if (unit == null) throw missingRequired(reader, Collections.singleton(Attribute.UNIT));
        requireNoContent(reader);
        return new TimeSpec(unit, qty);
    }

    protected static ScaledCount readScaledCountElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
        long perCpu = -1L;
        long count = -1L;
        final int acnt = reader.getAttributeCount();
        for (int i = 0; i < acnt; i ++) {
            switch (Attribute.forName(reader.getAttributeLocalName(i))) {
                case PER_CPU: {
                    perCpu = reader.getLongAttributeValue(i);
                    break;
                }
                case COUNT: {
                    count = reader.getLongAttributeValue(i);
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }
        }
        if (perCpu == -1L) throw missingRequired(reader, Collections.singleton(Attribute.PER_CPU));
        if (count == -1L) throw missingRequired(reader, Collections.singleton(Attribute.COUNT));
        requireNoContent(reader);
        return new ScaledCount(count, perCpu);
    }

    protected static void writeTimeSpecElement(final XMLExtendedStreamWriter writer, final TimeSpec timeSpec, final String localName) throws XMLStreamException {
        writer.writeEmptyElement(localName);
        writer.writeAttribute("time", Long.toString(timeSpec.getDuration()));
        writer.writeAttribute("unit", timeSpec.getUnit().toString().toLowerCase(Locale.ENGLISH));
    }

    protected static void writeScaledCountElement(final XMLExtendedStreamWriter writer, final ScaledCount scaledCount, final String localName) throws XMLStreamException {
        writer.writeEmptyElement(localName);
        final long count = scaledCount.getCount();
        if (count > 0L) writer.writeAttribute("count", Long.toString(count));
        final long perCpu = scaledCount.getPerCpu();
        if (perCpu > 0L) writer.writeAttribute("per-cpu", Long.toString(perCpu));
    }

    public long elementHash() {
        long hash = name.hashCode() & 0xFFFFFFFFL;
        if (threadFactory != null) hash = Long.rotateLeft(hash, 1) ^ threadFactory.hashCode() & 0xFFFFFFFFL;
        if (maxThreads != null) hash = Long.rotateLeft(hash, 1) ^ maxThreads.elementHash();
        if (keepaliveTime != null) hash = Long.rotateLeft(hash, 1) ^ keepaliveTime.elementHash();
        return hash;
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

    public PropertiesElement getProperties() {
        return properties;
    }

    void setProperties(final PropertiesElement properties) {
        this.properties = properties;
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
}
