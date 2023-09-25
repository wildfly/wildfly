/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ejb.client.reconnect;

import jakarta.ejb.Stateless;

/**
 * Simple SLSB test bean.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@Stateless
public class SimpleCrashBean implements SimpleCrashBeanRemote {

    public String echo(String message) {
        return message;
    }

}
