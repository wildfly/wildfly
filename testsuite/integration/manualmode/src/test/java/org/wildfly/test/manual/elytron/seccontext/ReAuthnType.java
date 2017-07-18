/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.manual.elytron.seccontext;

/**
 * Enum of ways which can be used for (re)authentication or identity propagation in Security context propagation tests.
 *
 * @author Josef Cacek
 */
public enum ReAuthnType {
    NO_REAUTHN, FORWARDED_IDENTITY, AUTHENTICATION_CONTEXT, SECURITY_DOMAIN_AUTHENTICATE, SECURITY_DOMAIN_AUTHENTICATE_FORWARDED
}
