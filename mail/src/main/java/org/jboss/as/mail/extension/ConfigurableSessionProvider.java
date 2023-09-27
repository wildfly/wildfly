/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

/**
 * A {@link SessionProvider that additionally exposes its configuration.
 * This is used only for test verification.
 * @author Paul Ferraro
 */
interface ConfigurableSessionProvider extends SessionProvider {
    MailSessionConfig getConfig();
}
