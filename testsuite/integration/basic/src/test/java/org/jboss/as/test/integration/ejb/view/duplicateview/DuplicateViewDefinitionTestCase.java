/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.view.duplicateview;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class DuplicateViewDefinitionTestCase {
    @Deployment
    public static Archive<?> deployment() {

        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "ejb3as7-1012.jar")
                .addPackage(AnnotatedDoNothingBean.class.getPackage())
                .addPackage(DoNothingBean.class.getPackage())
                .addAsManifestResource(DuplicateViewDefinitionTestCase.class.getPackage(), "ejb-jar.xml");
        return archive;
    }

    @EJB(mappedName = "java:global/ejb3as7-1012/AnnotatedDoNothingBean!org.jboss.as.test.integration.ejb.view.duplicateview.DoNothing")
    private DoNothing bean;

    @Test
    public void testDoNothing() {
        // if it deploys, we are good to go
        bean.doNothing();
    }
}
