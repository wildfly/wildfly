/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.inheritance;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
@LocalBean
public class InheritingBean extends AbstractBaseClass  {

    @Override
    public String sayGoodbye() {
        return "Goodbye";
    }

}
