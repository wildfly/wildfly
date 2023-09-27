/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.annotation;

import jakarta.annotation.Resource;
import jakarta.mail.Session;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class RepeatableMailSessionDefinitionTestCase {

    @Resource(mappedName = "java:/mail/test-mail-session-1")
    private Session session1;

    @Resource(mappedName = "java:/mail/test-mail-session-2")
    private Session session2;


    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(MailServlet.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testRepeatedMailSessionDefinition() {
        Assert.assertNotNull(session1);
        Assert.assertNotNull(session2);
    }


}
