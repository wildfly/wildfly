/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.model.host;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Policy options for how a slave host controller started in
 * {@link org.jboss.as.controller.RunningMode#ADMIN_ONLY admin-only mode} should
 * deal with the absence of a local copy of the domain-wide configuration.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public enum AdminOnlyDomainConfigPolicy {

    /** Start the HC with no domain wide configuration. */
    ALLOW_NO_CONFIG("allow-no-config"),
    /**
     * Contact the master host controller for the current domain wide configuration.
     * The host will not actually register with the master. If the master cannot
     * be reached, start of the host controller will fail.
     */
    FETCH_FROM_MASTER("fetch-from-master"),
    /**
     * This absence of a local copy domain wide config is not supported, and start
     * of the host controller will fail.
     */
    REQUIRE_LOCAL_CONFIG("require-local-config");

    public static final AdminOnlyDomainConfigPolicy DEFAULT = ALLOW_NO_CONFIG;

    private final String toString;

    private AdminOnlyDomainConfigPolicy(String toString) {
        this.toString = toString;
    }

    @Override
    public String toString() {
        return toString;
    }

    private static final Map<String, AdminOnlyDomainConfigPolicy> POLICY_MAP = new HashMap<String, AdminOnlyDomainConfigPolicy>();

    static {
        for (AdminOnlyDomainConfigPolicy policy : AdminOnlyDomainConfigPolicy.values()) {
            POLICY_MAP.put(policy.toString().toUpperCase(Locale.ENGLISH), policy);
        }
    }

    public static AdminOnlyDomainConfigPolicy getPolicy(String stringForm) {
        AdminOnlyDomainConfigPolicy result = POLICY_MAP.get(stringForm.toUpperCase(Locale.ENGLISH));
        if (result == null) {
            result = AdminOnlyDomainConfigPolicy.valueOf(stringForm);
        }
        return result;
    }
}
