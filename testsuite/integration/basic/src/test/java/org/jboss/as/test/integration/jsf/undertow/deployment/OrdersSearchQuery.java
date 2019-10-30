package org.jboss.as.test.integration.jsf.undertow.deployment;

import javax.enterprise.context.SessionScoped;
import javax.faces.model.ArrayDataModel;
import javax.faces.model.DataModel;
import javax.inject.Named;
import java.io.Serializable;


@Named
@SessionScoped
public class OrdersSearchQuery implements Serializable {

    private static final long serialVersionUID = 134313L;

    private static final SpecificOrder[] ORDERS = new SpecificOrder[] {
            new SpecificOrder("PersonalOrder-1", PersonalID.ID1),
            new SpecificOrder("PersonalOrder-2", PersonalID.ID2)
    };

    private transient DataModel<SpecificOrder> paidOrders = new ArrayDataModel<>(ORDERS);

    public DataModel<SpecificOrder> getPaidOrders() {
        return paidOrders;
    }
}
