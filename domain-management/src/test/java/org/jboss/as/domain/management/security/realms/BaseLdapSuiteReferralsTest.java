/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security.realms;

import static org.jboss.as.domain.management.security.realms.LdapTestSuite.HOST_NAME;
import static org.jboss.as.domain.management.security.realms.LdapTestSuite.MASTER_LDAP_PORT;
import static org.jboss.as.domain.management.security.realms.LdapTestSuite.SLAVE_LDAP_PORT;

import java.util.List;

import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition.ReferralHandling;
import org.jboss.as.domain.management.security.operations.OutboundConnectionAddBuilder;
import org.jboss.dmr.ModelNode;


/**
 * An extension of {@link BaseLdapSuiteTest} that instead adds two LDAP connections so that referrals can be tested.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class BaseLdapSuiteReferralsTest extends BaseLdapSuiteTest {

    public static final String SLAVE_CONNECTION_NAME = "SlaveConnection";
    public static final String DEFAULT_SEARCH_DN = "uid=wildfly,dc=simple,dc=wildfly,dc=org";
    public static final String DEFAULT_SEARCH_CREDENTIAL = "wildfly_password";

    @Override
    protected void addAddOutboundConnectionOperations(List<ModelNode> bootOperations) throws Exception {
        bootOperations.add(OutboundConnectionAddBuilder.builder(MASTER_CONNECTION_NAME)
                .setUrl("ldap://" + HOST_NAME + ":" + MASTER_LDAP_PORT)
                .setSearchDn(getSearchDn())
                .setSearchCredential(getSearchCredential())
                .setReferrals(getReferrals())
                .build());

        bootOperations.add(OutboundConnectionAddBuilder.builder(SLAVE_CONNECTION_NAME)
                .setUrl("ldap://" + HOST_NAME + ":" + SLAVE_LDAP_PORT)
                .setSearchDn(DEFAULT_SEARCH_DN)
                .setSearchCredential(DEFAULT_SEARCH_CREDENTIAL)
                .addHandlesReferralsFor("ldap://" + HOST_NAME + ":" + SLAVE_LDAP_PORT)
                .addHandlesReferralsFor("ldap://dummy:389")
                .build());
    }

    /*
     * These default values with with ReferralHandling set to FOLLOW as both
     * master and slave have the same identity defined.
     *
     * Tests can override all three of these methods to force it into THROW mode.
     */

    protected String getSearchDn() {
        return DEFAULT_SEARCH_DN;
    }

    protected String getSearchCredential() {
        return DEFAULT_SEARCH_CREDENTIAL;
    }

    protected ReferralHandling getReferrals() {
        return ReferralHandling.FOLLOW;
    }




}
