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

package org.wildfly.iiop.openjdk.csiv2;

import org.jboss.iiop.csiv2.SASCurrent;
import org.omg.CORBA.LocalObject;
import org.omg.CSI.IdentityToken;

/**
 * <p>
 * This class implements {@code SASCurrent}.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SASCurrentImpl extends LocalObject implements SASCurrent {

    private SASTargetInterceptor serverInterceptor;

    /**
     * <p>
     * Initialize the {@code SASCurrent} instance.
     * </p>
     *
     * @param serverInterceptor a reference to the {@code SASTargetInterceptor} that acts as a delegate for this
     *                          implementation.
     */
    public void init(SASTargetInterceptor serverInterceptor) {
        this.serverInterceptor = serverInterceptor;
    }

    @Override
    public boolean context_received() {
        return this.serverInterceptor.sasContextReceived();
    }

    @Override
    public boolean client_authentication_info_received() {
        return this.serverInterceptor.authenticationTokenReceived();
    }

    @Override
    public byte[] get_incoming_username() {
        return this.serverInterceptor.getIncomingUsername();
    }

    @Override
    public byte[] get_incoming_password() {
        return this.serverInterceptor.getIncomingPassword();
    }

    @Override
    public byte[] get_incoming_target_name() {
        return this.serverInterceptor.getIncomingTargetName();
    }

    @Override
    public IdentityToken get_incoming_identity() {
        return this.serverInterceptor.getIncomingIdentity();
    }

    @Override
    public int get_incoming_identity_token_type() {
        return this.serverInterceptor.getIncomingIdentity().discriminator();
    }

    @Override
    public byte[] get_incoming_principal_name() {
        return this.serverInterceptor.getIncomingPrincipalName();
    }

    @Override
    public void reject_incoming_context() {
        this.serverInterceptor.rejectIncomingContext();
    }
}