/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.view.duplicateview;

/**
 * Do not annotate this bean, that must happen in the individual test.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class DoNothingBean implements DoNothing {
    @Override
    public void doNothing() {
        // yep, it really does nothing
    }
}
