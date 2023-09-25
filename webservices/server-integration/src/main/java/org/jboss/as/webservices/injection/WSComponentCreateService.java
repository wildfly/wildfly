/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.injection;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.BasicComponentCreateService;
import org.jboss.as.ee.component.ComponentConfiguration;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSComponentCreateService extends BasicComponentCreateService {

    public WSComponentCreateService( final ComponentConfiguration componentConfiguration ) {
        super(componentConfiguration);
    }

    @Override
    protected BasicComponent createComponent() {
        return new WSComponent(this);
    }
}
