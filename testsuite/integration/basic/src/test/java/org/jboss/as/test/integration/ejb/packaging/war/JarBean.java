/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.packaging.war;

import jakarta.ejb.EJB;

/**
 * Stateless bean placed into jar archive in war archive.
 * Bean definition in xml dd.
 *
 * @author Ondrej Chaloupka
 */
public class JarBean implements BeanInterface {
    @EJB(beanName = "WarBean")
    private BeanInterface warBean;

    public String checkMe() {
        return "Hi " + warBean.say() + " from jar";
    }

    public String say() {
        return "jar";
    }
}
