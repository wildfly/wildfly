package org.jboss.as.test.integration.ejb.view.basic;

import java.io.Externalizable;
import java.io.Serializable;

import jakarta.ejb.Local;
import jakarta.ejb.Stateful;

/**
 * @author: Jaikiran Pai
 */
@Stateful
@Local
public class MultipleLocalViewBean extends MultipleRemoteViewBean implements One, Two, Three, Serializable, Externalizable {
}
