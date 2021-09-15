/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.sso;

import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author Paul Ferraro
 */
public class ElytronSSOServerSetupTask extends CLIServerSetupTask {
    public ElytronSSOServerSetupTask() {

        NodeBuilder nb = this.builder.node(AbstractClusteringTestCase.TWO_NODES)
                .setup("/subsystem=elytron/filesystem-realm=sso:add(path=sso-realm, relative-to=jboss.server.data.dir)")
                .setup("/subsystem=elytron/security-domain=sso:add(default-realm=sso, permission-mapper=default-permission-mapper,realms=[{realm=sso, role-decoder=groups-to-roles}])")
                .setup("/subsystem=elytron/http-authentication-factory=sso:add(security-domain=sso, http-server-mechanism-factory=global, mechanism-configurations=[{mechanism-name=FORM}])");
        // We already have an application-security-domain; need to reconfigure
        nb = nb.setup("/subsystem=undertow/application-security-domain=other:undefine-attribute(name=security-domain)")
                .setup("/subsystem=undertow/application-security-domain=other:write-attribute(name=http-authentication-factory,value=sso");

        nb = nb.setup("/subsystem=elytron/key-store=sso:add(path=sso.keystore, relative-to=jboss.server.config.dir, credential-reference={clear-text=password}, type=PKCS12)")
                .setup("/subsystem=undertow/application-security-domain=other/setting=single-sign-on:add(key-store=sso, key-alias=localhost, credential-reference={clear-text=password})")
                .teardown("/subsystem=undertow/application-security-domain=other/setting=single-sign-on:remove()")
                .teardown("/subsystem=elytron/key-store=sso:remove()");
        nb = nb.teardown("/subsystem=undertow/application-security-domain=other:undefine-attribute(name=http-authentication-factory)")
                .teardown("/subsystem=undertow/application-security-domain=other:write-attribute(name=security-domain,value=ApplicationDomain");

         nb.teardown("/subsystem=elytron/http-authentication-factory=sso:remove()")
                .teardown("/subsystem=elytron/security-domain=sso:remove()")
                .teardown("/subsystem=elytron/filesystem-realm=sso:remove()")
                ;
    }
}
