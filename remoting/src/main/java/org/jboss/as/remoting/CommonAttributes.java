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

    String AUTHENTICATION_PROVIDER = "authentication-provider";
    String CONNECTOR = "connector";
    String FORWARD_SECRECY = "forward-secrecy";
    String INCLUDE_MECHANISMS = "include-mechanisms";
    String LOCAL_OUTBOUND_CONNECTION = "local-outbound-connection";
    String NAME = "name";
    String NO_ACTIVE = "no-active";
    String NO_ANONYMOUS = "no-anonymous";
    String NO_DICTIONARY = "no-dictionary";
    String NO_PLAIN_TEXT = "no-plain-text";
    String OUTBOUND_CONNECTION = "outbound-connection";
    String OUTBOUND_SOCKET_BINDING_REF = "outbound-socket-binding-ref";
    String PASS_CREDENTIALS = "pass-credentials";
    String POLICY = "policy";
    String PROPERTIES = "properties";
    String PROPERTY = "property";
    String QOP = "qop";
    String REMOTE_OUTBOUND_CONNECTION = "remote-outbound-connection";
    String REUSE_SESSION= "reuse-session";
    String SASL = "sasl";
    String SASL_POLICY = "sasl-policy";
    String SECURITY = "security";
    String SECURITY_REALM = "security-realm";
    String SERVER_AUTH = "server-auth";
    String SOCKET_BINDING = "socket-binding";
    String STRENGTH = "strength";
    String SUBSYSTEM = "subsystem";
    String THREAD_POOL = "thread-pool";
    String URI = "uri";
    String USERNAME = "username";
    String VALUE = "value";
    String WORKER_READ_THREADS = "worker-read-threads";
    String WORKER_TASK_CORE_THREADS = "worker-task-core-threads";
    String WORKER_TASK_KEEPALIVE = "worker-task-keepalive";
    String WORKER_TASK_LIMIT = "worker-task-limit";
    String WORKER_TASK_MAX_THREADS = "worker-task-max-threads";
    String WORKER_WRITE_THREADS = "worker-write-threads";


}
