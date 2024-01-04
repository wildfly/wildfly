/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.security;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyContextException;

import org.jboss.metadata.ear.spec.EarMetaData;

/**
 * A service that creates JACC permissions for an ear deployment
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Scott.Stark@jboss.org
 * @author Anil.Saldhana@jboss.org
 */
public class EarJaccService extends JaccService<EarMetaData> {

    public EarJaccService(String contextId, EarMetaData metaData, Boolean standalone) {
        super(contextId, metaData, standalone);
    }

    /** {@inheritDoc} */
    @Override
    public void createPermissions(EarMetaData metaData, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        // nothing to do
    }

}
