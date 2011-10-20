/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.transaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Tests that java:comp/UserTransaction is bound to JNDI for web requests
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class UserTransactionBindingTest {

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "tranaction.war");
        war.addPackage(UserTransactionBindingTest.class.getPackage());
        war.addClass(HttpRequest.class);
        return war;
    }


    private static String performCall(String urlPattern) throws Exception {
        return HttpRequest.get("http://localhost:8080/tranaction/" + urlPattern, 10, SECONDS);
    }

    @Test
    public void testUserTransactionBound() throws Exception {
        Assert.assertEquals("true", performCall("simple"));
    }

}
