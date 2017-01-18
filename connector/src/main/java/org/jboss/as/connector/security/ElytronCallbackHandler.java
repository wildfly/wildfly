/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.security;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.jca.core.spi.security.Callback;

/**
 * CallbackHandler implementation for Elytron
 *
 * @author Flavia Rainone
 */
public class ElytronCallbackHandler implements CallbackHandler, Serializable {

    /** Callback mappings */
    private Callback mappings;

    /**
     * Constructor
     */
    public ElytronCallbackHandler() {
        this(null);
    }

    /**
     * Constructor
     * @param mappings The mappings
     */
    public ElytronCallbackHandler(Callback mappings) {
        this.mappings = mappings;
    }

    /**
     * {@inheritDoc}
     */
    public void handle(javax.security.auth.callback.Callback[] callbacks) throws UnsupportedCallbackException, IOException {
        if (SUBSYSTEM_RA_LOGGER.isTraceEnabled())
            SUBSYSTEM_RA_LOGGER.elytronHandlerHandle(Arrays.toString(callbacks));

        if (callbacks != null && callbacks.length > 0)
        {
            if (mappings != null)
            {
                callbacks = mappings.mapCallbacks(callbacks);
            }
            // TODO
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ElytronCallbackHandler@").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("[mappings=").append(mappings);
        sb.append("]");

        return sb.toString();
    }
}
