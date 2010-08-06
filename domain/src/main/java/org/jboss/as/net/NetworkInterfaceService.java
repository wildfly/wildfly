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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.jboss.as.model.socket.InterfaceElement;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Emanuel Muckenhuber
 */
public class NetworkInterfaceService implements Service<NetworkInterfaceBinding> {

	public static final ServiceName JBOSS_NETWORK_INTERFACE = ServiceName.JBOSS.append("network");
	
	private static final boolean preferIPv4Stack = Boolean.getBoolean("java.net.preferIPv4Stack"); 

	/** The interface binding. */
	private NetworkInterfaceBinding binding;
	
	/** The network interface element. */
	private final InterfaceElement interfaceElement;

	public NetworkInterfaceService(final InterfaceElement element) {
		this.interfaceElement = element;
	}

	public synchronized void start(StartContext arg0) throws StartException {
		try {
			if(interfaceElement.getAddress() != null) {
				final InetAddress address = InetAddress.getByName(interfaceElement.getAddress());
				final NetworkInterface net = NetworkInterface.getByInetAddress(address);
				this.binding = new NetworkInterfaceBinding(net, address);
			} else {
				this.binding = resolveInterface(interfaceElement);
			}
		} catch(Exception e) {
			throw new StartException(e);
		}
		if(this.binding == null) {
			throw new StartException("failed to resolve interface for " + interfaceElement.getName()
					+ ", " + interfaceElement.getLocation()); 
		}
	}

	public synchronized void stop(StopContext arg0) {
		this.binding = null;
	}

	public synchronized NetworkInterfaceBinding getValue() throws IllegalStateException {
		final NetworkInterfaceBinding binding = this.binding;
		if(binding == null) {
			throw new IllegalStateException();
		}
		return binding;
	}
	
	static NetworkInterfaceBinding resolveInterface(final InterfaceElement element) throws SocketException {
		final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			final NetworkInterface networkInterface = networkInterfaces.nextElement();
			final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
			while (interfaceAddresses.hasMoreElements()) {
				final InetAddress address = interfaceAddresses.nextElement();
				if (element.getInterfaceCriteria().isAcceptable(networkInterface, address)) {
					final InetAddress resolved = getInterfaceAddress(networkInterface);
					if (resolved != null) {
						return new NetworkInterfaceBinding(networkInterface, resolved);
					}
				}
			}
		}
		return null;
	}
	
	static InetAddress getInterfaceAddress(final NetworkInterface networkInterface) {
		final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
		while(interfaceAddresses.hasMoreElements()) {
			final InetAddress address = interfaceAddresses.nextElement();
			// prefer IPv4 stack
			if(preferIPv4Stack && address instanceof Inet4Address) {
				return address;
			} else if (! preferIPv4Stack) {
				return address;
			}
		}
		return null;
	}
	
}

