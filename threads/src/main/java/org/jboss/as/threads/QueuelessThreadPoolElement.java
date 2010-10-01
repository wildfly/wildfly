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

import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class QueuelessThreadPoolElement extends AbstractExecutorElement<QueuelessThreadPoolElement> {

    private static final long serialVersionUID = -8281883758711778557L;

    private String handoffExecutor;
    private boolean blocking;

    public QueuelessThreadPoolElement(final String name) {
        super(name);
    }

    @Override
    protected Class<QueuelessThreadPoolElement> getElementClass() {
        return QueuelessThreadPoolElement.class;
    }

    public String getHandoffExecutor() {
        return handoffExecutor;
    }

    public boolean isBlocking() {
        return blocking;
    }

    void setHandoffExecutor(final String handoffExecutor) {
        this.handoffExecutor = handoffExecutor;
    }

    void setBlocking(final boolean blocking) {
        this.blocking = blocking;
    }

    protected void writeAttributes(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        super.writeAttributes(streamWriter);
        streamWriter.writeAttribute("blocking", Boolean.toString(blocking));
    }

    protected void writeElements(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        super.writeElements(streamWriter);
        if (handoffExecutor != null) {
            streamWriter.writeEmptyElement("handoff-executor");
            streamWriter.writeAttribute("name", handoffExecutor);
        }
    }

    QueuelessThreadPoolAdd getAdd() {
        final QueuelessThreadPoolAdd add = new QueuelessThreadPoolAdd(getName(), getMaxThreads());
        add.setBlocking(isBlocking());
        add.setHandoffExecutor(getHandoffExecutor());
        add.setKeepaliveTime(getKeepaliveTime());
        add.setThreadFactory(getThreadFactory());
        add.getProperties().putAll(getProperties());
        return add;
    }
}
