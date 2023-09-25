/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
