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

import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * @author Emanuel Muckenhuber
 */
public final class NetworkInterfaceBinding {

	private final InetAddress address;
	private final NetworkInterface networkInterface;

	NetworkInterfaceBinding(NetworkInterface networkInterface, InetAddress address) {
		this.address = address;
		this.networkInterface = networkInterface;
	}

	/**
	 * Get the network address.
	 *
	 * @return the network address
	 */
	public InetAddress getAddress() {
		return this.address;
	}

	/**
	 * Get the network interface.
	 *
	 * @return the network interface
	 */
	public NetworkInterface getNetworkInterface() {
		return this.networkInterface;
	}

}
