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

package org.jboss.as.server.manager.management;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassTable;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;

/**
 * Abstract class to allow extensions to avoid duplicating {@link org.jboss.marshalling.Marshaller}/{@link org.jboss.marshalling.Unmarshaller}
 * creation and logic.
 *
 * @author John Bailey
 */
public abstract class AbstractManagementOperationHandler implements ManagementOperationHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");
    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;

    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", ModuleClassLoader.forModuleName("org.jboss.marshalling.river"));
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
        final MarshallingConfiguration config = new MarshallingConfiguration();
        config.setClassTable(ModularClassTable.getInstance());
        CONFIG = config;
    }

    /**
     * Create a {@link org.jboss.marshalling.Marshaller} and {@link org.jboss.marshalling.Unmarshaller} from the provided screams.
     * Then delegate to the implementation to actually handle the request using the {@link org.jboss.marshalling.Unmarshaller}
     * The implementation must provide a {@link org.jboss.as.server.manager.management.AbstractManagementOperationHandler.OperationResponse}
     * used to respond to the requester.
     *
     * @param inputStream The InputStream of the request
     * @param outputStream The OutputStream of the request.
     */
    public void handleRequest(final InputStream inputStream, final OutputStream outputStream) {
        Unmarshaller unmarshaller = null;
        Marshaller marshaller = null;
        try {
            unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
            unmarshaller.start(Marshalling.createByteInput(inputStream));
            final OperationResponse response = handle(unmarshaller);
            unmarshaller.finish();

            marshaller = MARSHALLER_FACTORY.createMarshaller(CONFIG);
            marshaller.start(Marshalling.createByteOutput(outputStream));
            response.handle(marshaller);
            marshaller.finish();
        } catch (Throwable t) {
            log.error("Failed to process server manager request", t);
        } finally {
            safeClose(unmarshaller);
            safeClose(marshaller);
        }
    }

    /**
     * Handle a request by using the provided {@link org.jboss.marshalling.Unmarshaller} to attain
     * any information from the request, and return an {@link org.jboss.as.server.manager.management.AbstractManagementOperationHandler.OperationResponse}
     * that can be used to write back the requester.
     *
     * @param unmarshaller The unmarshaller for the request
     * @return A response to write to the requester
     */
    protected abstract OperationResponse handle(final Unmarshaller unmarshaller);

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable ignored) {
            // todo: log me
        }
    }

    /**
     * Response object used to allow a response to be marahalled to the requester.
     */
    protected static interface OperationResponse {
        /**
         * Handle writting the respones to the requester.
         *
         * @param marshaller The marshaller to write the response to
         * @throws Exception If any errors occur marshaling the response
         */
        void handle(final Marshaller marshaller) throws Exception;
    }

    /**
     * A no-op operation response.
     */
    protected static OperationResponse NO_OP_RESPONSE = new OperationResponse() {
        public void handle(Marshaller marshaller) {
        }
    };
}
