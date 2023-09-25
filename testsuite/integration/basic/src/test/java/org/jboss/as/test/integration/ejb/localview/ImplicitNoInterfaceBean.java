/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.localview;

import java.io.Serializable;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
public class ImplicitNoInterfaceBean implements Serializable {
    public void message() {

    }
}
