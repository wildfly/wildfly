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
package org.jboss.as.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author Emanuel Muckenhuber
 */
public class ManagedServerSocketBinding extends ServerSocket implements ManagedBinding {

	private final SocketBindingManager manager;
	
	ManagedServerSocketBinding(final SocketBindingManager manager) throws IOException {
		this.manager = manager;
	}
	
	public InetSocketAddress getBindAddress() {
		return InetSocketAddress.class.cast(getLocalPort());
	}

	public void bind(SocketAddress endpoint, int backlog) throws IOException {
		super.bind(endpoint, backlog);
		manager.registerSocket(this);
	}
	
	public Socket accept() throws IOException {
		final ManagedSocketBinding socket = new ManagedSocketBinding(manager);
		implAccept(socket);
		return socket;
	}

	public void close() throws IOException {
		try {
			super.close();
		} finally {
			manager.unregisterSocket(this);
		}
	}
	
}

