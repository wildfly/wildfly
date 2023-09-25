/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.jndi;

import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

/**
 * @author Jaikiran Pai
 */
@Stateful
@LocalBean
@Remote(RemoteEcho.class)
@Local (Echo.class)
// TODO: Add other views too like @LocalHome and @RemoteHome
public class SampleSFSB implements  RemoteEcho {


    @Override
    public String echo(String msg) {
        return msg;
    }
}
