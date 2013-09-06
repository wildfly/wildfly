/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cli.accesscontrol;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.parsing.ParserUtil;

/**
 * @author Alexey Loubyansky
 *
 */
public abstract class AddressAccessRequirement extends BaseAccessRequirement {

    protected final OperationRequestAddress address;

    AddressAccessRequirement() {
        address = new DefaultOperationRequestAddress();
    }

    AddressAccessRequirement(String address) {
        this.address = new DefaultOperationRequestAddress();
        if (address != null) {
            final CommandLineParser.CallbackHandler handler = new DefaultCallbackHandler(this.address);
            try {
                ParserUtil.parseOperationRequest(address, handler);
            } catch (CommandFormatException e) {
                throw new IllegalArgumentException("Failed to parse path '" + address + "': " + e.getMessage());
            }
        }
    }

    AddressAccessRequirement(OperationRequestAddress address) {
        if(address == null) {
            throw new IllegalArgumentException("address is null");
        }
        this.address = address;
    }

    protected OperationRequestAddress getAddress() {
        return address;
    }
}
