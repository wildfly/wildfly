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
public final class BoundedQueueThreadPoolElement extends AbstractExecutorElement<BoundedQueueThreadPoolElement> {

    private static final long serialVersionUID = -6314205265652284301L;

    private String handoffExecutor;
    private boolean blocking;
    private boolean allowCoreTimeout;
    private ScaledCount queueLength;
    private ScaledCount coreThreads;

    public BoundedQueueThreadPoolElement(final String name) {
        super(name);
    }

    @Override
    protected Class<BoundedQueueThreadPoolElement> getElementClass() {
        return BoundedQueueThreadPoolElement.class;
    }

    public String getHandoffExecutor() {
        return handoffExecutor;
    }

    void setHandoffExecutor(final String handoffExecutor) {
        this.handoffExecutor = handoffExecutor;
    }

    public boolean isBlocking() {
        return blocking;
    }

    void setBlocking(final boolean blocking) {
        this.blocking = blocking;
    }

    public boolean isAllowCoreTimeout() {
        return allowCoreTimeout;
    }

    void setAllowCoreTimeout(final boolean allowCoreTimeout) {
        this.allowCoreTimeout = allowCoreTimeout;
    }

    public ScaledCount getQueueLength() {
        return queueLength;
    }

    void setQueueLength(final ScaledCount queueLength) {
        this.queueLength = queueLength;
    }

    public ScaledCount getCoreThreads() {
        return coreThreads;
    }

    void setCoreThreads(final ScaledCount coreThreads) {
        this.coreThreads = coreThreads;
    }

    protected void writeAttributes(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        super.writeAttributes(streamWriter);
        if (blocking) { streamWriter.writeAttribute("blocking", "true"); }
        if (allowCoreTimeout) { streamWriter.writeAttribute("allow-core-timeout", "true"); }
    }

    protected void writeElements(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (handoffExecutor != null) {
            streamWriter.writeEmptyElement("handoff-executor");
            streamWriter.writeAttribute("name", handoffExecutor);
        }
        if (coreThreads != null) {
            writeScaledCountElement(streamWriter, coreThreads, "core-threads");
        }
        writeScaledCountElement(streamWriter, queueLength, "queue-length");
        super.writeElements(streamWriter);
    }

    AbstractThreadsSubsystemUpdate<Void> getAdd() {
        final BoundedQueueThreadPoolAdd add = new BoundedQueueThreadPoolAdd(getName(), getMaxThreads(), getQueueLength());
        add.setKeepaliveTime(getKeepaliveTime());
        add.setThreadFactory(getThreadFactory());
        add.setBlocking(blocking);
        add.setHandoffExecutor(handoffExecutor);
        add.setAllowCoreTimeout(allowCoreTimeout);
        add.setCoreThreads(coreThreads);
        add.getProperties().putAll(getProperties());
        return add;
    }
}
