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

package org.jboss.as.test.manualmode.vault.module;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;

/**
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CustomSecurityVault implements SecurityVault {

	private volatile Map<String, Object> options;

	@Override
	public void init(Map<String, Object> options) throws SecurityVaultException {
		this.options = Collections.synchronizedMap(new HashMap<String, Object>());
		this.options.putAll(options);
	}

	@Override
	public boolean isInitialized() {
		return options != null;
	}

	@Override
	public byte[] handshake(Map<String, Object> handshakeOptions)
			throws SecurityVaultException {
		//Not relevant for this test
		return null;
	}

	@Override
	public Set<String> keyList() throws SecurityVaultException {
		return options.keySet();
	}

	@Override
	public boolean exists(String vaultBlock, String attributeName)
			throws SecurityVaultException {
		//Not relevant for this test
		return false;
	}

	@Override
	public void store(String vaultBlock, String attributeName,
			char[] attributeValue, byte[] sharedKey)
			throws SecurityVaultException {

	}

	@Override
	public char[] retrieve(String vaultBlock, String attributeName,
			byte[] sharedKey) throws SecurityVaultException {
	    System.out.println("------> block " + vaultBlock);
	    System.out.println("------> attr " + attributeName);
		Object o = options.get(vaultBlock);
		if (o != null){
		    String val = o.toString() + "_" + vaultBlock + "_" + attributeName + "_" + new String(sharedKey, StandardCharsets.UTF_8);
			return val.toCharArray();
		}
		return null;
	}

	@Override
	public boolean remove(String vaultBlock, String attributeName,
			byte[] sharedKey) throws SecurityVaultException {
		return false;
	}



}
