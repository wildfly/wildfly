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
package org.jboss.as.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.model.socket.SocketBindingElement;

/**
 * @author Emanuel Muckenhuber
 */
public class ServiceBindingManager  {

	private final int portOffSet;
	private final Map<String, InetAddress> resolvedInterfaces;
	private final Map<String, SocketBindingElement> socketBindings;

	private final Map<InetSocketAddress, ManagedBinding> managedBindings = new ConcurrentHashMap<InetSocketAddress, ManagedBinding>();
	
	ServiceBindingManager(final Map<String, SocketBindingElement> socketBindings, final Map<String, InetAddress> interfaces, final int portOffSet) {
		this.socketBindings = Collections.synchronizedMap(socketBindings);
		this.resolvedInterfaces = Collections.synchronizedMap(interfaces);
		this.portOffSet = portOffSet;
	}

	/**
	 * Resolve the {@code InetSocketAddress} for a given binding name.
	 * 
	 * @param bindingName the socket binding name.
	 * @return the socket address
	 */
	public InetSocketAddress resolveSocketAddress(String bindingName) {
		final SocketBindingElement binding = socketBindings.get(bindingName);
		if(binding == null) {
			throw new IllegalArgumentException("failed to resolve " + bindingName);
		}
		final InetAddress address = resolvedInterfaces.get(binding.getInterfaceName());
		if(address == null) {
			throw new IllegalStateException("failed to resolve interface address for binding " + bindingName);
		}
		int port = binding.getPort();
		if(binding.isFixedPort() == false) {
			port += portOffSet;
		}
		return new InetSocketAddress(address, port);
	}
	
	/**
	 * Resolve the multicast {@code InetSocketAddress} for a given binding name.
	 * 
	 * @param bindingName the socket binding name
	 * @return the socket address
	 */
	public InetSocketAddress resolveMulticastSocketAddress(String bindingName) {
		final SocketBindingElement binding = socketBindings.get(bindingName);
		if(binding == null) {
			throw new IllegalArgumentException("failed to resolve " + bindingName);
		}
		if(binding.getMulticastAddress() == null) {
			throw new IllegalStateException("no multicast address configured for " + binding.getName());
		}
		final InetAddress address = binding.getMulticastAddress();
		final int port = binding.getMulticastPort();
		return new InetSocketAddress(address, port);
	}
	
	/**
	 * @return the registered bindings
	 */
	public Collection<ManagedBinding> listBindings() {
		return managedBindings.values();
	}
	
	/**
	 * Register an active socket binding.
	 * 
	 * @param binding the managed binding
	 * @param bindingName the binding name
	 */
	public void registerSocket(ManagedBinding binding) {
		managedBindings.put(binding.getBindAddress(), binding);
	}
	
	/**
	 * Unregister a socket binding.
	 * 
	 * @param binding the managed socket binding
	 */
	public void unregisterSocket(ManagedBinding binding) {
		managedBindings.remove(binding.getBindAddress());
	}
	
}
