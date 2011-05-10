/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

/**
 * Attributes used by the security subsystem.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface Constants {

    String ACL = "acl";
    String ADDITIONAL_PROPERTIES = "additional-properties";
    String AUDIT = "audit";
    String AUDIT_MANAGER_CLASS_NAME = "audit-manager-class-name";
    String AUTH_MODULE = "auth-module";
    String AUTHENTICATION = "authentication";
    String AUTHENTICATION_JASPI = "authentication-jaspi";
    String AUTHENTICATION_MANAGER_CLASS_NAME = "authentication-manager-class-name";
    String AUTHORIZATION = "authorization";
    String AUTHORIZATION_MANAGER_CLASS_NAME = "authorization-manager-class-name";
    String CACHE_TYPE = "cache-type";
    String CIPHER_SUITES = "cipher-suites";
    String CLIENT_ALIAS = "client-alias";
    String CLIENT_AUTH = "client-auth";
    String CODE = "code";
    String DEEP_COPY_SUBJECT_MODE = "deep-copy-subject-mode";
    String DEFAULT_CALLBACK_HANDLER_CLASS_NAME = "default-callback-handler-class-name";
    String EXTENDS = "extends";
    String FLAG = "flag";
    String IDENTITY_TRUST = "identity-trust";
    String IDENTITY_TRUST_MANAGER_CLASS_NAME = "identity-trust-manager-class-name";
    String JSSE = "jsse";
    String KEY_MANAGER_FACTORY_ALGORITHM = "key-manager-factory-algorithm";
    String KEY_MANAGER_FACTORY_PROVIDER = "key-manager-factory-provider";
    String KEYSTORE_PASSWORD = "keystore-password";
    String KEYSTORE_PROVIDER = "keystore-provider";
    String KEYSTORE_PROVIDER_ARGUMENT = "keystore-provider-argument";
    String KEYSTORE_TYPE = "keystore-type";
    String KEYSTORE_URL = "keystore-url";
    String LOGIN_MODULE_STACK = "login-module-stack";
    String LOGIN_MODULE_STACK_REF = "login-module-stack-ref";
    String MAPPING = "mapping";
    String MAPPING_MANAGER_CLASS_NAME = "mapping-manager-class-name";
    String MODULE_OPTIONS = "module-options";
    String NAME = "name";
    String PROTOCOLS = "protocols";
    String SECURITY_MANAGEMENT = "security-management";
    String SECURITY_DOMAIN = "security-domain";
    String SERVER_ALIAS = "server-alias";
    String SERVICE_AUTH_TOKEN = "service-auth-token";
    String SUBJECT_FACTORY = "subject-factory";
    String SUBJECT_FACTORY_CLASS_NAME = "subject-factory-class-name";
    String TRUST_MANAGER_FACTORY_ALGORITHM = "trust-manager-factory-algorithm";
    String TRUST_MANAGER_FACTORY_PROVIDER = "trust-manager-factory-provider";
    String TRUSTSTORE_PASSWORD = "truststore-password";
    String TRUSTSTORE_PROVIDER = "truststore-provider";
    String TRUSTSTORE_PROVIDER_ARGUMENT = "truststore-provider-argument";
    String TRUSTSTORE_TYPE = "truststore-type";
    String TRUSTSTORE_URL = "truststore-url";
    String TYPE = "type";
    String VALUE = "value";
}
