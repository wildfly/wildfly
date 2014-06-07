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

package org.jboss.as.jdkorb.security;

import org.jboss.iiop.ssl.SSLPolicy;
import org.jboss.iiop.ssl.SSLPolicyValue;
import org.jboss.iiop.ssl.SSL_POLICY_TYPE;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;

public class SSLPolicyImpl extends LocalObject implements SSLPolicy {
    private SSLPolicyValue value;

    public SSLPolicyImpl(SSLPolicyValue value) {
        this.value = value;
    }

    public SSLPolicyValue value() {
        return value;
    }

    public int policy_type() {
        return SSL_POLICY_TYPE.value;
    }

    public Policy copy() {
        return new SSLPolicyImpl(value);
    }

    public void destroy() {
    }

    public String toString() {
        return "SSLPolicy[" + ((value == SSLPolicyValue.SSL_NOT_REQUIRED) ? "SSL_NOT_REQUIRED" : "SSL_REQUIRED") + "]";
    }
}
