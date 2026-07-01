/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.arquillian.assertj;

import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * An {@link org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender} that appends @{code org.assertj}
 * and its dependencies to the archive.
 *
 * @see <a href="https://github.com/arquillian/arquillian-core/issues/859">https://github.com/arquillian/arquillian-core/issues/859</a>
 * @author Radoslav Husar
 */
class AssertJArchiveAppender extends CachedAuxilliaryArchiveAppender {
    @Override
    protected Archive<?> buildArchive() {
        return ShrinkWrap.create(JavaArchive.class, "wildfly-integration-tests-assertj.jar")
                .addPackages(true, "org.assertj")
                .addPackages(true, "net.bytebuddy")
                ;
    }
}
