/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

/**
 * Interface defining when a group request is done, allowing early termination of a group request based on logic implemented by
 * the caller. For example, an RPC could be invoked on all members of a group, but as soon as a single valid response is
 * received the {@link #needMoreResponses()} method could return <code>false</code> allowing the call to return without waiting
 * for other slower-to-arrive responses.
 *
 * Based on JGroups' RspFilter concept, but with a separate abstraction. The abstraction has be done to avoid leaking JGroups
 * classes to ha-server-api consumers and to avoid adding an absolute dependency on JGroups to HAPartition implementations.
 *
 * @see HAPartition#callMethodOnCluster(String, String, Object[], Class[], boolean, ResponseFilter)
 *
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 */
public interface ResponseFilter {
    /**
     * Determines whether a response from a given sender should be added to the response list of the request
     *
     * @param response The response (usually a serializable value)
     * @param sender The sender of response
     * @return <code>true</code> if we should add the response to the response list of returned by
     *         {@link HAPartition#callMethodOnCluster(String, String, Object[], Class[], boolean, ResponseFilter)} otherwise
     *         false. In the latter case, the response will not be included in the list returned to the RPC caller.
     */
    boolean isAcceptable(Object response, ClusterNode sender);

    /**
     * Right after a call to {@link #isAcceptable(Object, ClusterNode)}, this method is called to see whether we are done with
     * the request and can unblock the caller.
     *
     * @return <code>true</code> if the request is done, otherwise <code>false</code>alse
     */
    boolean needMoreResponses();
}
