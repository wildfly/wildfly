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
package org.jboss.as.test.integration.jpa.hibernate.envers;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class ValidityAuditStrategyonInheritanceTestCase {

    private static final String ARCHIVE_NAME = "jpa_TestValidityAuditStrategyonInheritance";

    private static final String persistence_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
            + "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">"
            + "  <persistence-unit name=\"myPlayer\">" + "    <description>Persistence Unit." + "    </description>"
            + "    <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" + "    <properties> "
            + "      <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" + "    </properties>"
            + "    <properties> " + "      <property name=\"org.hibernate.envers.audit_strategy\" "
            + "      value=\"org.hibernate.envers.strategy.ValidityAuditStrategy\"/>"
            + "      <property name=\"org.hibernate.envers.audit_strategy_validity_revend_timestamp_field_name\" "
            + "      value=\"REVEND_VALIDITY\"/>" + "    </properties>" + "  </persistence-unit>" + "</persistence>";

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
        jar.add(new StringAsset(persistence_xml), "META-INF/persistence.xml");
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
