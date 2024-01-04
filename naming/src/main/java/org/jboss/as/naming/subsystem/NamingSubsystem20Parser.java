/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.subsystem;

/**
 * @author Eduardo Martins
 */
class NamingSubsystem20Parser extends NamingSubsystem14Parser {

    NamingSubsystem20Parser() {
        super(NamingSubsystemNamespace.NAMING_2_0);
    }
}
