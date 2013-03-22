package org.jboss.as.web.host;


/**
 * This is basically just a hack for the OSGi HttpService that
 * allows different deployments to share a servlet context.
 *
 * The type of object that returned from the wrapper must be the same as
 * the object that is returned.
 *
 *
 * @author Stuart Douglas
 */
public interface ApplicationContextWrapper {

    Object wrap(final Object context);
}
