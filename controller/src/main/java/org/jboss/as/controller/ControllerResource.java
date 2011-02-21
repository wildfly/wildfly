/**
 *
 */
package org.jboss.as.controller;

/**
 * TODO add class javadoc for ControllerResource
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public interface ControllerResource {

    void commit();

    void rollback();
}
