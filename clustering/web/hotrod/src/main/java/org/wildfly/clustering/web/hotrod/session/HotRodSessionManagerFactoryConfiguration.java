/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.hotrod.session;

import org.wildfly.clustering.web.session.SessionManagerFactoryConfiguration;

/**
 * @param <S> the HttpSession specification type
 * @param <SC> the ServletContext specification type
 * @param <AL> the HttpSessionAttributeListener specification type
 * @param <LC> the local context type
 * @author Paul Ferraro
 */
public interface HotRodSessionManagerFactoryConfiguration<S, SC, AL, LC> extends SessionManagerFactoryConfiguration<S, SC, AL, LC>, HotRodSessionFactoryConfiguration {
}
