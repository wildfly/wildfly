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
package org.jboss.as.host.controller;


import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.msc.service.Service;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.io.IOException;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ServerInventoryCallbackService implements CallbackHandler, Service<CallbackHandler> {

    static final ServiceName SERVICE_NAME = ServerInventoryService.SERVICE_NAME.append("callback");

    static void install(final ServiceTarget serviceTarget) {
        final ServerInventoryCallbackService callbackService = new ServerInventoryCallbackService();
        serviceTarget.addService(ServerInventoryCallbackService.SERVICE_NAME, callbackService)
                .setInitialMode(ON_DEMAND)
                .install();
    }

    private volatile CallbackHandler callbackHandler;

    public void start(StartContext startContext) throws StartException {
        //
    }

    public void stop(StopContext stopContext) {
        //
    }

    public CallbackHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Set the callback handler, once the server inventory is started.
     *
     * @param callbackHandler the server inventory callback handler
     */
    protected void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        final CallbackHandler callbackHandler = this.callbackHandler;
        if(callbackHandler != null) {
            callbackHandler.handle(callbacks);
        }
    }
}
