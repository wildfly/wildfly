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
package org.jboss.as.services.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Emanuel Muckenhuber
 */
public class SocketBindingManagerService implements SocketBindingManager, Service<SocketBindingManager> {

	private final int portOffSet;
	private final SocketFactory socketFactory = new ManagedSocketFactory();
	private final ServerSocketFactory serverSocketFactory = new ManagedServerSocketFactory();

	private final Map<InetSocketAddress, ManagedBinding> managedBindings = new ConcurrentHashMap<InetSocketAddress, ManagedBinding>();

	public SocketBindingManagerService(int portOffSet) {
		this.portOffSet = portOffSet;
	}
	
	public void start(StartContext context) throws StartException {
		//
	}
	
	public void stop(StopContext context) {
		
	}

	public SocketBindingManager getValue() throws IllegalStateException {
		return this;
	}
	
	public int getPortOffset() {
		return portOffSet;
	}
	
	/**
	 * Get the managed server socket factory.
	 * 
	 * @return the server socket factory
	 */
	public ServerSocketFactory getServerSocketFactory() {
		return serverSocketFactory;
	}
	
	/**
	 * Get the socket factory.
	 * 
	 * @return the socket factory
	 */
	public SocketFactory getSocketFactory() {
		return socketFactory;
	}
	
	/**
	 * Create a datagram socket.
	 * 
	 * @param address the socket address
	 * @return the datagram socket
	 * @throws SocketException
	 */
	public DatagramSocket createDatagramSocket(SocketAddress address) throws SocketException {
		return new ManagedDatagramSocketBinding(this, address);
	}
	
	/**
	 * Create a multicast socket.
	 * 
	 * @param address the socket address
	 * @return the multicast socket 
	 * @throws IOException
	 */
	public MulticastSocket createMulticastSocket(SocketAddress address) throws IOException {
		return new ManagedMulticastSocketBinding(this, address);
	}

	/**
	 * @return the registered bindings
	 */
	public Collection<ManagedBinding> listActiveBindings() {
		return managedBindings.values();
	}
	
	/**
	 * Register an active socket binding.
	 * 
	 * @param binding the managed binding
	 * @param bindingName the binding name
	 */
	public void registerBinding(ManagedBinding binding) {
		managedBindings.put(binding.getBindAddress(), binding);
	}
	
	/**
	 * Unregister a socket binding.
	 * 
	 * @param binding the managed socket binding
	 */
	public void unregisterBinding(ManagedBinding binding) {
		managedBindings.remove(binding.getBindAddress());
	}
	
	class ManagedSocketFactory extends SocketFactory {

		public Socket createSocket() {
			return new ManagedSocketBinding(SocketBindingManagerService.this);
		}
	
		public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
			return createSocket(InetAddress.getByName(host), port);
		}

		public Socket createSocket(final InetAddress host, final int port) throws IOException {
			final Socket socket = createSocket();
			socket.connect(new InetSocketAddress(host, port));
			return socket;
		}

		public Socket createSocket(final String host, final int port, final InetAddress localHost, final int localPort) throws IOException, UnknownHostException {
			return createSocket(InetAddress.getByName(host), port, localHost, localPort);
		}

		public Socket createSocket(final InetAddress address, final int port, final InetAddress localAddress, final int localPort) throws IOException {
	        final Socket socket = createSocket();
	        socket.bind(new InetSocketAddress(localAddress, localPort));
	        socket.connect(new InetSocketAddress(address, port));
	        return socket;
		}
	}

	class ManagedServerSocketFactory extends ServerSocketFactory {

	    public ServerSocket createServerSocket() throws IOException {
	        return new ManagedServerSocketBinding(SocketBindingManagerService.this);
	    }

	    public ServerSocket createServerSocket(final int port) throws IOException {
	        final ServerSocket serverSocket = createServerSocket();
	        serverSocket.bind(new InetSocketAddress(port));
	        return serverSocket;
	    }

	    public ServerSocket createServerSocket(final int port, final int backlog) throws IOException {
	        final ServerSocket serverSocket = createServerSocket();
	        serverSocket.bind(new InetSocketAddress(port), backlog);
	        return serverSocket;
	    }

	    public ServerSocket createServerSocket(final int port, final int backlog, final InetAddress ifAddress) throws IOException {
	        final ServerSocket serverSocket = createServerSocket();
	        serverSocket.bind(new InetSocketAddress(ifAddress, port), backlog);
	        return serverSocket;
	    }
	}
	
}

