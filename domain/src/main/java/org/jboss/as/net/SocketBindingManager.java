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
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collection;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public interface SocketBindingManager {
	
	public static final ServiceName SOCKET_BINDING_MANAGER = ServiceName.JBOSS.append("socket-binding-manager");
	
	/**
	 * Get the server port offset.
	 * TODO move to somewhere else...
	 * 
	 * @return the port offset
	 */
	int getPortOffset();
	
	/**
	 * Get the managed server socket factory.
	 * 
	 * @return the server socket factory
	 */
	public ServerSocketFactory getServerSocketFactory();
	
	/**
	 * Get the socket factory.
	 * 
	 * @return the socket factory
	 */
	public SocketFactory getSocketFactory();
	
	/**
	 * Create a datagram socket.
	 * 
	 * @param address the socket address
	 * @return the datagram socket
	 * @throws SocketException
	 */
	public DatagramSocket createDatagramSocket(SocketAddress address) throws SocketException ;
	
	/**
	 * Create a multicast socket.
	 * 
	 * @param address the socket address
	 * @return the multicast socket 
	 * @throws IOException
	 */
	public MulticastSocket createMulticastSocket(SocketAddress address) throws IOException;

	/**
	 * @return the registered bindings
	 */
	public Collection<ManagedBinding> listActiveBindings();
	
	/**
	 * Register an active socket binding.
	 * 
	 * @param binding the managed binding
	 * @param bindingName the binding name
	 */
	public void registerSocket(ManagedBinding binding);
	
	/**
	 * Unregister a socket binding.
	 * 
	 * @param binding the managed socket binding
	 */
	public void unregisterSocket(ManagedBinding binding);

}

