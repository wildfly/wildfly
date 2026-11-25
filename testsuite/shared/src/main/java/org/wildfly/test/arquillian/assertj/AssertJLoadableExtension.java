/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.arquillian.assertj;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * A {@link LoadableExtension} that registers {@link AssertJArchiveAppender}.
 *
 * @see <a href="https://github.com/arquillian/arquillian-core/issues/859">https://github.com/arquillian/arquillian-core/issues/859</a>
 * @author Radoslav Husar
 */
public class AssertJLoadableExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.service(AuxiliaryArchiveAppender.class, AssertJArchiveAppender.class);
    }
}
