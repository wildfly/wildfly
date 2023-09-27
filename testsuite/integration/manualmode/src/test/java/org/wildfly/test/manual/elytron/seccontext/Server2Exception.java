/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Custom exception that should be available just in deployments on server2.
 *
 * @author Ondrej Kotek
 */
public class Server2Exception extends RuntimeException {

    public Server2Exception() {
        super();
    }

    public Server2Exception(String s) {
        super(s);
    }
}
