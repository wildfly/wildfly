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

        NodeBuilder nb = this.builder.node(AbstractClusteringTestCase.NODE_1_2.toArray(new String[0]))
                .setup("/subsystem=elytron/filesystem-realm=sso:add(path=sso-realm, relative-to=jboss.server.data.dir)")
                .setup("/subsystem=elytron/security-domain=sso:add(default-realm=sso, permission-mapper=default-permission-mapper,realms=[{realm=sso, role-decoder=groups-to-roles}])");

        nb = nb.setup("/subsystem=undertow/application-security-domain=sso-domain:add(security-domain=sso)");

        nb = nb.setup("/subsystem=elytron/key-store=sso:add(path=sso.keystore, relative-to=jboss.server.config.dir, credential-reference={clear-text=password}, type=PKCS12)")
                .setup("/subsystem=undertow/application-security-domain=sso-domain/setting=single-sign-on:add(key-store=sso, key-alias=localhost, credential-reference={clear-text=password})")
                .teardown("/subsystem=undertow/application-security-domain=sso-domain:remove()")
                .teardown("/subsystem=elytron/key-store=sso:remove()");

         nb.teardown("/subsystem=elytron/security-domain=sso:remove()")
                .teardown("/subsystem=elytron/filesystem-realm=sso:remove()")
                ;
    }
}
