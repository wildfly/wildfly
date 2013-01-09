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

package org.jboss.as.iiop.csiv2.idl;

/**
 * Generated from IDL interface "SASCurrent".
 *
 * @author JacORB IDL compiler V 2.3.0 (JBoss patch 4), 06-Jun-2007
 * @version generated at Apr 17, 2011 3:27:19 PM
 */
@SuppressWarnings("unused")
public class SASCurrentLocalTie extends _SASCurrentLocalBase {

    private SASCurrentOperations _delegate;

    public SASCurrentLocalTie(SASCurrentOperations delegate) {
        _delegate = delegate;
    }

    public SASCurrentOperations _delegate() {
        return _delegate;
    }

    public void _delegate(SASCurrentOperations delegate) {
        _delegate = delegate;
    }

    public byte[] get_incoming_password() {
        return _delegate.get_incoming_password();
    }

    public byte[] get_incoming_principal_name() {
        return _delegate.get_incoming_principal_name();
    }

    public boolean context_received() {
        return _delegate.context_received();
    }

    public boolean client_authentication_info_received() {
        return _delegate.client_authentication_info_received();
    }

    public void reject_incoming_context() {
        _delegate.reject_incoming_context();
    }

    public byte[] get_incoming_target_name() {
        return _delegate.get_incoming_target_name();
    }

    public byte[] get_incoming_username() {
        return _delegate.get_incoming_username();
    }

    public org.omg.CSI.IdentityToken get_incoming_identity() {
        return _delegate.get_incoming_identity();
    }

    public int get_incoming_identity_token_type() {
        return _delegate.get_incoming_identity_token_type();
    }

}
