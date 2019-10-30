/*
 * Copyright 2017 Red Hat, Inc.
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
package org.jboss.as.connector.metadata.api.common;

/**
 * Extension of {@link org.jboss.jca.common.api.metadata.common.SecurityMetadata} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public interface SecurityMetadata extends org.jboss.jca.common.api.metadata.common.SecurityMetadata {
   /**
    *
    * Indicates if Elytron will be used to authenticate connections.
    *
    * @return {@code true} if Elytron is enabled
    */
   boolean isElytronEnabled();

}
