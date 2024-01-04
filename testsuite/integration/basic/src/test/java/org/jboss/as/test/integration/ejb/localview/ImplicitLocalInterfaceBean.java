/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.localview;

import java.io.Serializable;
import jakarta.ejb.Stateless;

/**
 * Bean that has a single implicit local interface, that also implements Serializable
 *
 * @author Stuart Douglas
 */
@Stateless
public class ImplicitLocalInterfaceBean  implements Serializable, ImplicitLocalInterface {

    @Override
    public void message() {

    }
}
