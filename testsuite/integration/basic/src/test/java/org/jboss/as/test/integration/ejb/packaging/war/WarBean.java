/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.packaging.war;

import jakarta.ejb.EJB;

/**
 * Stateless bean placed into classes in war archive. Bean definition in xml dd.
 *
 * @author Ondrej Chaloupka
 */
public class WarBean implements BeanInterface {
    @EJB
    private JarBean jarBean;

    public String checkMe() {
        return "Hi " + jarBean.say() + " from war";
    }

    public String say() {
        return "war";
    }
}
