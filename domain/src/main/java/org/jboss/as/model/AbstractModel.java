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

package org.jboss.as.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jboss.marshalling.FieldSetter;
import org.jboss.msc.service.Location;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * A controlled object model which is related to an XML representation.  Such an object model can be serialized to
 * XML or to binary.
 *
 * @param <M> the concrete model type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractModel<M extends AbstractModel<M>> extends AbstractModelRootElement<M> {

    private static final long serialVersionUID = 66064050420378211L;

    /**
     * The complete set of elements within this model.
     */
    private transient final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Construct a new instance.
     *
     * @param location the declaration location of this model
     * @param elementName the root element name
     */
    protected AbstractModel(final Location location, final QName elementName) {
        super(location, elementName);
    }

    /**
     * Construct a new instance.
     *
     * @param reader the reader from which to build this element
     * @throws XMLStreamException if an error occurs
     */
    protected AbstractModel(final XMLExtendedStreamReader reader) throws XMLStreamException {
        super(reader);
    }

    /**
     * Get the difference between this model element and another, as a list of updates which, when applied, would make
     * this model element equivalent to the other.  Locks this model, then the other model, for read.
     *
     * @param other the other model element
     * @return the collection of updates
     */
    public final List<AbstractModelUpdate<M>> getDifference(M other) {
        lockForRead();
        try {
            other.lockForRead();
            try {
                final List<AbstractModelUpdate<M>> list = new ArrayList<AbstractModelUpdate<M>>();
                appendDifference(list, other);
                return list;
            } finally {
                other.unlockForRead();
            }
        } finally {
            unlockForRead();
        }
    }

    /**
     * Synchronize this model with the other model.  Gets all the differences with the other and merges them into this
     * one, one update at a time, under a single lock.
     *
     * @param other the other model
     */
    public final void synchronize(M other) {
        lockForWrite();
        try {
            for (AbstractModelUpdate<M> update : getDifference(other)) {
                update.applyUpdate(cast());
            }
        } finally {
            unlockForWrite();
        }
    }

    // Protected members

    @SuppressWarnings({ "LockAcquiredButNotSafelyReleased" })
    protected final void lockForRead() {
        readWriteLock.readLock().lock();
    }

    protected final void unlockForRead() {
        readWriteLock.readLock().unlock();
    }

    @SuppressWarnings({ "LockAcquiredButNotSafelyReleased" })
    protected final void lockForWrite() {
        readWriteLock.writeLock().lock();
    }

    protected final void unlockForWrite() {
        readWriteLock.writeLock().unlock();
    }

    // Serialization

    private static final FieldSetter readWriteLockSetter = FieldSetter.get(AbstractModel.class, "readWriteLock");

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        readWriteLockSetter.set(this, new ReentrantReadWriteLock());
        ois.defaultReadObject();
    }
}
