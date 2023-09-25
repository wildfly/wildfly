/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.jndi;

import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@LocalBean
@Local (Echo.class)
@Remote(RemoteEcho.class)
@EJB(name = "java:global/Additional", beanName = "SampleSLSB", beanInterface = Echo.class)
// TODO: Add all other views @LocalHome and @RemoteHome
public class SampleSLSB implements Echo {

    @Override
    public String echo(String msg) {
        return msg;
    }
}
