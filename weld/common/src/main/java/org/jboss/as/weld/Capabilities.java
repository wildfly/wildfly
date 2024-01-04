/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

/**
 * Capability names exposed by Weld subsystem.
 * <p>
 * This class is designed to be used outside of the weld subsystem. Other subsystems that require the name of a Weld
 * capability can safely import this class without adding a jboss module dependency. To archive it, this class must
 * only declare string constants, which are resolved at compile time.
 *
 * @author Yeray Borges
 */
public final class Capabilities {
    public static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
}
