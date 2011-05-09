/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jboss.as.clustering;

import java.util.concurrent.Future;

/**
 * A GroupCommunicationService that is able to help applications provide current service state to newly joined group members.
 * <p>
 * All nodes that expect to be able to {@link #getServiceState(String) receive state} must also
 * {@link #registerStateTransferProvider(String, StateTransferProvider) register to provide state}.
 *
 * @see StateTransferProvider
 *
 * @author Brian Stansberry
 *
 * @version $Revision$
 */
public interface GroupStateTransferService extends GroupCommunicationService {
    /**
     * Gets the current service state from an existing member of the group. Intended to be called by newly joining members of
     * the group.
     * <p>
     * Equivalent to <code>getServiceState(serviceName, null)</code>.
     * </p>
     *
     * @param serviceName the name of the service
     *
     * @return <code>Future</code> whose {@link Future#get() get()} method will return the service state when it becomes
     *         available.
     */
    Future<SerializableStateTransferResult> getServiceState(String serviceName);

    /**
     * Gets the current service state from an existing member of the group, using the given classloader to deserialize it.
     * Intended to be called by newly joining members of the group.
     *
     * @param serviceName the name of the service
     * @param classloader the ClassLoader to use to deserialize the state when it becomes available. May be <code>null</code> in
     *        which case the GroupStateTransferService implementation class' classloader will be used.
     *
     * @return <code>Future</code> whose {@link Future#get() get()} method will return the service state when it becomes
     *         available.
     */
    Future<SerializableStateTransferResult> getServiceState(String serviceName, ClassLoader classloader);

    /**
     * Gets an input stream from which can be read the current service state from an existing member of the group. Intended to
     * be called by newly joining members of the group.
     * <p>
     * Equivalent to <code>getServiceState(serviceName, null)</code>.
     * </p>
     *
     * @param serviceName the name of the service
     *
     * @return <code>Future</code> whose {@link Future#get() get()} method will return the service state when it becomes
     *         available.
     */
    Future<StreamStateTransferResult> getServiceStateAsStream(String serviceName);

    /**
     * Registers the object that can provide state for the service when newly joining group members request it.
     *
     * @param serviceName the name of the service
     * @param provider the state provider
     */
    void registerStateTransferProvider(String serviceName, StateTransferProvider provider);

    /**
     * Unregisters the object that can provide state for the service.
     *
     * @param serviceName the name of the service
     */
    void unregisterStateTransferProvider(String serviceName);
}
