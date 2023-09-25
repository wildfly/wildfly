/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;

/**
 * @author Stuart Douglas
 */
public class EjbHomeViewDescription extends EJBViewDescription {

    public EjbHomeViewDescription(final ComponentDescription componentDescription, final String viewClassName, final MethodInterfaceType methodIntf) {
        super(componentDescription, viewClassName, methodIntf, false);
    }

}
