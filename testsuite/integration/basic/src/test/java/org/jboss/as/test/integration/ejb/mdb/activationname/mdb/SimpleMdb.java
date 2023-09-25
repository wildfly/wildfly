/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.activationname.mdb;

import org.jboss.as.test.integration.ejb.mdb.activationname.adapter.SimpleListener;
import org.jboss.ejb3.annotation.ResourceAdapter;

import jakarta.ejb.MessageDriven;

/**
 * @author Ivo Studensky
 */
@MessageDriven
@ResourceAdapter("ear-with-simple-adapter.ear#simple-adapter.rar")
public class SimpleMdb implements SimpleListener {

    @Override
    public void onMessage() {
        // nothing to do here
    }

}
