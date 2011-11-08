/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.as.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * A representation of a socket binding. Open ports need to be registered
 * using the {@code SocketBindingManager}.
 *
 * @author Emanuel Muckenhuber
 */
public interface ManagedBinding extends Closeable {

    /**
     * Get the optional socket binding configuration name.
     *
     * @return the socket binding name, <code>null</code> if not available
     */
    String getSocketBindingName();

    /**
     * Get the bind address.
     *
     * @return the bind address.
     */
    InetSocketAddress getBindAddress();

    /**
     * Close and unregister this binding.
     *
     * @throws IOException if an I/O error occurs
     */
    void close() throws IOException;

    final class Factory {
        public static ManagedBinding createSimpleManagedBinding(final String name, final InetSocketAddress socketAddress, final Closeable closeable) {
            if (socketAddress == null) {
                throw new IllegalArgumentException("socketAddress is null");
            }
            return new ManagedBinding() {

                @Override
                public String getSocketBindingName() {
                    return name;
                }

                @Override
                public InetSocketAddress getBindAddress() {
                    return socketAddress;
                }

                @Override
                public void close() throws IOException {
                    if (closeable != null) {
                        closeable.close();
                    }
                }
            };
        }

        public static ManagedBinding createSimpleManagedBinding(final SocketBinding socketBinding) {
            if (socketBinding == null) {
                throw new IllegalArgumentException("socketBinding is null");
            }
            return new ManagedBinding() {

                @Override
                public String getSocketBindingName() {
                    return socketBinding.getName();
                }

                @Override
                public InetSocketAddress getBindAddress() {
                    return socketBinding.getSocketAddress();
                }

                @Override
                public void close() throws IOException {
                    // no-op
                }
            };
        }
    }

}

