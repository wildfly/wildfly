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

public class IdentityServerSetupTask extends CLIServerSetupTask {
    public IdentityServerSetupTask() {
        this.builder.node(AbstractClusteringTestCase.NODE_1_2.toArray(new String[0]))
                .setup("/subsystem=elytron/filesystem-realm=sso:add-identity(identity=user1)")
                .setup("/subsystem=elytron/filesystem-realm=sso:add-identity-attribute(identity=user1, name=groups, value=[Users])")
                .setup("/subsystem=elytron/filesystem-realm=sso:set-password(identity=user1, clear={password=password1})")
                .teardown("/subsystem=elytron/filesystem-realm=sso:remove-identity(identity=user1)")
                ;
    }
}
