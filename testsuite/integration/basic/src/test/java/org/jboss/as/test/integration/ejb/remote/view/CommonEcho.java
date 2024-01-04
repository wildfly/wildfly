/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.view;

/**
 * @author Jaikiran Pai
 */
abstract class CommonEcho implements LocalEcho {

    @Override
    public String echo(String message) {
        return message;
    }
}
