/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.testsuite.integration.secman.custompermissions;

import java.security.BasicPermission;

/**
 *
 * @author Hynek Švábek <hsvabek@redhat.com>
 *
 */
public class CustomPermission extends BasicPermission {

    private static final long serialVersionUID = 1L;

    public CustomPermission(String name) {
        super(name);
    }
}