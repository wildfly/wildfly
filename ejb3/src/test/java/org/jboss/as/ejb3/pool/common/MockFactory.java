/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.pool.common;

import org.jboss.as.ejb3.pool.StatelessObjectFactory;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class MockFactory implements StatelessObjectFactory<MockBean> {
    public MockBean create() {
        MockBean bean = new MockBean();
        bean.postConstruct();
        return bean;
    }

    public void destroy(MockBean obj) {
        obj.preDestroy();
    }

}
