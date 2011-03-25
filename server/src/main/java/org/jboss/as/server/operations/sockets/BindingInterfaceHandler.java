/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.operations.sockets;

import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;

/**
 * Handler for changing the interface on a socket binding.
 *
 * TODO see comment on JBAS-9100 re: only requiring restart if there is an actual
 * active socket associated with the binding.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BindingInterfaceHandler extends ServerWriteAttributeOperationHandler implements BootOperationHandler {

    public static final BindingInterfaceHandler INSTANCE = new BindingInterfaceHandler();

    private BindingInterfaceHandler() {
        super(new StringLengthValidator(1, Integer.MAX_VALUE, true, true));
    }

}
