/*
 * Copyright 2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.wildfly.test.integration.vdx.utils.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which specifies server config file from resources directory with which server should be started.
 * <p>
 * If domain is tested then it allows to specify host.xml file to be used. Otherwise it's ignored.
 * <p>
 * Used during tryStartAndWaitForFail of @see Server#tryStartAndWaitForFail()
 * <p>
 * Created by mnovak on 10/24/16.
 */
@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME) public @interface ServerConfig {

    /**
     * Specifies with which configuration option server will be started.
     * <p>
     * Default value for standalone mode is "standalone.xml"
     * Default value for domain mode is "domain.xml"
     */
    String configuration() default "standalone.xml";

    /**
     * Optional - will be applied only in domain mode. Same as --host-config=...
     * <p>
     * Default is host.xml
     */
    String hostConfig() default "host.xml";

    String xmlTransformationGroovy() default "";

    //for things like subtree("webservices", Subtree.subsystemInProfile("profileXY", "webservices")).build());

    /**
     * subtree name used in .groovy file
     */
    String subtreeName() default "";

    // mapping to server configuration .xml file

    /**
     * server's subsystem
     */
    String subsystemName() default "";

    /**
     * server's profile, applicable only for domain
     */
    String profileName() default "";

    // provide variable name and value for transformation script

    /**
     * variable name for transformation script
     */
    String parameterName() default "foo";

    /**
     * variable value for transformation script
     */
    String parameterValue() default "bar";

}
