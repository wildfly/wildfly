/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    static final String JWT_CAPABILITY_NAME = "org.wildlfly.microprofile.jwt";

    /*
     * External Capabilities
     */

    static final String CONFIG_CAPABILITY_NAME = "org.wildfly.microprofile.config";

    static final String EE_SECURITY_CAPABILITY_NAME = "org.wildfly.ee.security";

    static final String ELYTRON_CAPABILITY_NAME = "org.wildfly.security.elytron";

    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";

}
