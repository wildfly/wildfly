/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.methodparams;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class TxMethodParamsTest {

    @Deployment
    public static EnterpriseArchive deploy() {
        return ShrinkWrap.create(EnterpriseArchive.class)
                .addAsModule(
                        ShrinkWrap.create(JavaArchive.class, "ejbs.jar")
                                .addPackage("org.jboss.as.test.integration.ejb.transaction.methodparams")
                                .addAsManifestResource(TxMethodParamsTest.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                );
    }

    @Test
    public void shouldNotThrowNeverNeverContainerTransactionWithoutMethodParams() throws Exception {
        LocalHome localHome = (LocalHome) new InitialContext().lookup("java:app/ejbs/FirstWithoutParams!org.jboss.as.test.integration.ejb.transaction.methodparams.LocalHome");
        assertThat(localHome.create().test(42), is(true));
        assertThat(localHome.create().test("Hello World"), is(true));
        assertThat(localHome.create().test(new String[0]), is(true));
    }

    @Test
    public void shouldNotThrowNeverNeverContainerTransactionWithMethodParams() throws Exception {
        LocalHome localHome = (LocalHome) new InitialContext().lookup("java:app/ejbs/FirstWithParams!org.jboss.as.test.integration.ejb.transaction.methodparams.LocalHome");
        assertThat(localHome.create().test(42), is(true));
        assertThat(localHome.create().test("Hello World"), is(true));
        assertThat(localHome.create().test(new String[0]), is(true));
    }

}
