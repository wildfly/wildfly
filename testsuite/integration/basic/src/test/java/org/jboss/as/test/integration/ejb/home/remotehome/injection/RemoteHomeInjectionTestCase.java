/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.home.remotehome.injection;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.home.remotehome.SimpleInterface;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the
 */
@RunWith(Arquillian.class)
public class RemoteHomeInjectionTestCase {

    private static final String ARCHIVE_NAME = "SimpleLocalHomeTest.war";


    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(RemoteHomeInjectionTestCase.class.getPackage());
        war.addPackage(SimpleInterface.class.getPackage());
        return war;
    }

    @Test
    public void testRemoteHomeInjection() throws Exception {
        final InjectingEjb ejb = (InjectingEjb) iniCtx.lookup("java:module/" + InjectingEjb.class.getSimpleName());
        Assert.assertEquals("OtherEjb", ejb.getMessage());
    }

}
