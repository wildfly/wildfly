/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ee;

/**
 * Exposes a mechanism to handle batching.
 * @author Paul Ferraro
 */
public interface Batcher<B extends Batch> {
    /**
     * Creates a batch.
     * @return a batch.
     */
    B createBatch();

    /**
     * Resumes a batch. Used if the specified batch was (or may have been) created by another thread.
     * @param batch an existing batch
     * @return the context of the resumed batch
     */
    BatchContext resumeBatch(B batch);

    /**
     * Suspends a batch.
     * @return the previously active batch, or null if there was no active batch
     */
    B suspendBatch();
}
