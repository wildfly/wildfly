/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.injection.deployment;

/**
 *
 * @author Emmanuel Hugonnet (c) 2021 Red Hat, Inc.
 */
public class DefaultJMSConnectionFactoryBinding {
    public static final String DEFAULT_JMS_CONNECTION_FACTORY = "DefaultJMSConnectionFactory";
    public static final String COMP_DEFAULT_JMS_CONNECTION_FACTORY = "java:comp/"+DEFAULT_JMS_CONNECTION_FACTORY;
}
