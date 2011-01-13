/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

/**
 * @author Emanuel Muckenhuber
 */
interface CommonAttributes {

    String ADD_CONNECTOR = "add-connector";
    String AUTHENTICATION_PROVIDER = "authentication-provider";
    String CONNECTOR = "connector";
    String FORWARD_SECRECY = "forward-secrecy";
    String INCLUDE_MECHANISMS = "include-mechanisms";
    String NO_ACTIVE = "no-active";
    String NO_ANONYMOUS = "no-anonymous";
    String NO_DICTIONARY = "no-dictionary";
    String NO_PLAINTEXT = "no-plaintext";
    String PASS_CREDENTIALS = "pass-credentials";
    String POLICY = "policy";
    String PROPERTIES = "properties";
    String PROPERTY = "property";
    String QOP = "qop";
    String REUSE_SESSION= "reuse-session";
    String SASL = "sasl";
    String SERVER_AUTH = "server-auth";
    String SOCKET_BINDING = "socket-binding";
    String STRENGTH = "strength";
    String SUBSYSTEM = "subsystem";
    String THREAD_POOL = "thread-pool";
    String VALUE = "value";

}
