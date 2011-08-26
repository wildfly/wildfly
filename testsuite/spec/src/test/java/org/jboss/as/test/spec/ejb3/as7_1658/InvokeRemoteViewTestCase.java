/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.as.test.spec.ejb3.as7_1658;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.spec.ejb3.as7_1658.a.ABean;
import org.jboss.as.test.spec.ejb3.as7_1658.a.ARemote;
import org.jboss.as.test.spec.ejb3.as7_1658.b.BBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class InvokeRemoteViewTestCase {
    @Deployment
    public static Archive<?> createA() {
        return ShrinkWrap.create(JavaArchive.class, "as7_1658-a.jar")
                .addPackage(ABean.class.getPackage());
    }

    @Deployment(name = "as7_1658-b")
    public static Archive<?> createB() {
        return ShrinkWrap.create(JavaArchive.class, "as7_1658-b.jar")
                .addPackage(BBean.class.getPackage())
                .addClass(ARemote.class);
    }

    @Test
    @OperateOnDeployment("as7_1658-b")
    public void testInvoke() throws Exception {
        final InitialContext ctx = new InitialContext();
        final BBean bean = (BBean) ctx.lookup("java:global/as7_1658-b/BBean");
        bean.doNothing();
    }
}
