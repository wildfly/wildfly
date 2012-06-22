package org.jboss.as.osgi;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * OSGi deployment properties marker.
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Jun-2012
 */
@Documented
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface DeploymentMarker {

    /**
     * Defines the start level for this bundle deployment.
     */
    boolean autoStart() default true;
}
