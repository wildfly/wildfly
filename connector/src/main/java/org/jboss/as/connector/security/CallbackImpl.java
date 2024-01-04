/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.security;

import java.util.Map;

/**
 * Extension of CallbackImpl with added support for Elytron.
 *
 * @author Flavia Rainone
 */
public class CallbackImpl extends org.jboss.jca.core.security.CallbackImpl {

    private boolean elytronEnabled;

    public CallbackImpl(boolean mappingRequired, String domain, String defaultPrincipal, String[] defaultGroups,
            Map<String, String> principals, Map<String, String> groups) {
        super(mappingRequired, domain, defaultPrincipal, defaultGroups, principals, groups);
    }
}
