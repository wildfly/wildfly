package org.jboss.as.arquillian.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation that can be used to inject a container specific resource.
 *
 * @author Stuart Douglas
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerResource {

    /**
     * The container to connect to. This may be left empty if only 1 server is available
     */
    String value() default "";

}
