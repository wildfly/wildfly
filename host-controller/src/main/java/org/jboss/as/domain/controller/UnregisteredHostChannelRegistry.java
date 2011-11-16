/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.domain.controller;

import org.jboss.as.controller.ProxyController;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementMessageHandler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface UnregisteredHostChannelRegistry {

    /**
     * Register a host channel to be registered in the DomainController
     *
     * @param hostName the name of the host
     * @param channel the channel
     * @param callback to be called when {@link UnregisteredHostChannelRegistry#popChannelAndCreateProxy(String)}
     * is called and creates a proxy
     * @throws IllegalArgumentException if there is already a channel for the hostName
     */
    void registerChannel(String hostName, ManagementChannel channel, ProxyCreatedCallback callback);

    /**
     * Get and remove a host channel to be registered in the DomainController
     * and create a proxy controller from it
     *
     * @param hostName the name of the host
     * @return the channel
     * @throws IllegalArgumentException if there is no channel for the hostName
     */
    ProxyController popChannelAndCreateProxy(String hostName);


    //TODO Kabir: Ugly but all I have time for now
    /**
     * Called when {@link UnregisteredHostChannelRegistry#popChannelAndCreateProxy(String)} creates a proxy
     * to get hold of the proxies ManagementOperationHandler
     *
     * @param handler the handler of the created proxy
     */
    interface ProxyCreatedCallback {
        /**
         * Gets called with the management operation handler once the proxy has been created
         *
         * @param handler the operation handler
         */
        void proxyCreated(ManagementMessageHandler handler);
    }

}
