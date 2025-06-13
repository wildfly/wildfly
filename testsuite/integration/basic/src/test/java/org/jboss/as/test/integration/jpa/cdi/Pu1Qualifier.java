package org.jboss.as.test.integration.jpa.cdi;

import jakarta.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Associatiate Pu1Qualifier with persistence unit: pu1
 *
 * @author Scott Marlow
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface Pu1Qualifier {

}
