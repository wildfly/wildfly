/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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