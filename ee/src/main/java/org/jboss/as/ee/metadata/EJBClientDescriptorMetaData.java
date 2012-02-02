/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.metadata;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Metadata for configurations contained in jboss-ejb-client.xml descriptor
 *
 * @author Jaikiran Pai
 */
public class EJBClientDescriptorMetaData {

    private boolean excludeLocalReceiver;
    private Boolean localReceiverPassByValue;

    private Set<String> remotingReceiverConnectionRefs = new HashSet<String>();

    /**
     * Adds a outbound connection reference used by a remoting receivers in the client context represented
     * by this {@link EJBClientDescriptorMetaData}
     *
     * @param outboundConnectionRef The name of the outbound connection. Cannot be null or empty string.
     */
    public void addRemotingReceiverConnectionRef(final String outboundConnectionRef) {
        if (outboundConnectionRef == null || outboundConnectionRef.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot add a remoting receiver which references a null/empty outbound connection");
        }
        this.remotingReceiverConnectionRefs.add(outboundConnectionRef);
    }

    /**
     * Returns a collection of outbound connection references that are used by the remoting receivers
     * configured in the client context of this {@link EJBClientDescriptorMetaData}
     *
     * @return
     */
    public Collection<String> getRemotingReceiverConnectionRefs() {
        return this.remotingReceiverConnectionRefs;
    }

    /**
     * Set the pass-by-value semantics for the local receiver belonging to the EJB client context
     * represented by this metadata
     *
     * @param passByValue True if pass-by-value. False otherwise.
     */
    public void setLocalReceiverPassByValue(final Boolean passByValue) {
        this.localReceiverPassByValue = passByValue;
    }

    /**
     * If pass-by-value semantics for the local EJB receiver has been explicitly set, then returns that value.
     * Else returns null.
     *
     * @return
     */
    public Boolean isLocalReceiverPassByValue() {
        return this.localReceiverPassByValue;
    }

    /**
     * Exclude/include the local receiver in the EJB client context represented by this metadata.
     *
     * @param excludeLocalReceiver True if local receiver has to be excluded in the EJB client context. False otherwise.
     */
    public void setExcludeLocalReceiver(final boolean excludeLocalReceiver) {
        this.excludeLocalReceiver = excludeLocalReceiver;
    }

    /**
     * Returns true if the local receiver is disabled in the EJB client context represented by this metadata.
     * Else returns false.
     *
     * @return
     */
    public boolean isLocalReceiverExcluded() {
        return this.excludeLocalReceiver;
    }
}
