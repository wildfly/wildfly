/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.hibernate.envers.validityauditstrategyoninheritancetest;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.hibernate.envers.Player;
import org.jboss.as.test.integration.jpa.hibernate.envers.SLSBAuditInheritance;
import org.jboss.as.test.integration.jpa.hibernate.envers.SoccerPlayer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Auditing on inherited attributes using Validity Audit strategy
 *
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class ValidityAuditStrategyonInheritanceTestCase {

    private static final String ARCHIVE_NAME = "jpa_TestValidityAuditStrategyonInheritance";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {

        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(Player.class, SoccerPlayer.class, SLSBAuditInheritance.class);
        jar.addAsManifestResource(ValidityAuditStrategyonInheritanceTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType
                .cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    /* Ensure that auditing works for inherited attributes */
    @Test
    public void testValidityStrategyonInheritance() throws Exception {

        SLSBAuditInheritance slsb = lookup("SLSBAuditInheritance", SLSBAuditInheritance.class);

        SoccerPlayer socplayer = slsb.createSoccerPlayer("LEONARDO", "MESSI", "SOCCER", "REAL MADRID");

        socplayer.setFirstName("Christiano");
        socplayer.setLastName("Ronaldo");
        socplayer.setGame("FOOTBALL");
        // update Player
        socplayer = slsb.updateSoccerPlayer(socplayer);

        SoccerPlayer val = slsb.retrieveSoccerPlayerbyId(socplayer.getId());
        Assert.assertNotNull(val);
        Assert.assertEquals("LEONARDO", val.getFirstName());
        Assert.assertEquals("MESSI", val.getLastName());

        Assert.assertNull(val.getGame());

    }

}
