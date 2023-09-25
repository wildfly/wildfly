/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.jwt.smallrye;

/**
 * Class to hold the capabilities as used or exposed by this subsystem.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class Capabilities {

    /*
     * Our Capabilities
     */

    static final String JWT_CAPABILITY_NAME = "org.wildfly.microprofile.jwt";

    /*
     * External Capabilities
     */

    static final String CONFIG_CAPABILITY_NAME = "org.wildfly.microprofile.config";

    static final String EE_SECURITY_CAPABILITY_NAME = "org.wildfly.ee.security";

    static final String ELYTRON_CAPABILITY_NAME = "org.wildfly.security.elytron";

    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";

}
