/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.methodparams;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
