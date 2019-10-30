/*
 * Copyright 2017 Red Hat, Inc.
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
package org.jboss.as.test.integration.management.jca;

/**
 * Type of connection security.
 *
 * @author Flavia Rainone
 */
public enum ConnectionSecurityType {
    // PicketBox security domain
    SECURITY_DOMAIN,
    // PicketBox security domain and application
    SECURITY_DOMAIN_AND_APPLICATION,
    // Application managed security
    APPLICATION,
    // Elytron managed security, with current authentication context
    ELYTRON,
    // Elytron managed security, with specified authentication context
    ELYTRON_AUTHENTICATION_CONTEXT,
    // Elytron and Application managed security, with specified authentication context
    ELYTRON_AUTHENTICATION_CONTEXT_AND_APPLICATION,
    // user name and password
    USER_PASSWORD
}
