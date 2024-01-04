/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.view.javapackage;

import java.util.concurrent.Callable;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class ViewFromJavaPackageTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-java-package.jar");
        jar.addPackage(ViewFromJavaPackageTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testViewInJavaPackage() throws Exception {
        final Context ctx = new InitialContext();
        final Callable t = (Callable) ctx.lookup("java:module/" + CallableEjb.class.getSimpleName());
        Assert.assertEquals(CallableEjb.MESSAGE, t.call());
    }

}
