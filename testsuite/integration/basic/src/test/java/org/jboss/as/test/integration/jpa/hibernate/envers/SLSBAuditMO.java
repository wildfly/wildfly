package org.jboss.as.test.integration.jpa.hibernate.envers;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

/**
 * @author Madhumita Sadhukhan
 */
@Stateless
public class SLSBAuditMO {
    @PersistenceContext(unitName = "myCustPhone")
    EntityManager em;

    public CustomerMO createCustomer(String firstName, String surName, String type, String areacode, String phnumber) {
        PhoneMO phone1 = new PhoneMO();
        phone1.setNumber(phnumber);
        phone1.setAreacode(areacode);
        phone1.setType(type);

        PhoneMO phone2 = new PhoneMO();
        phone2.setNumber("777222123");
        phone2.setAreacode("+420");
        phone2.setType("HOME");

        CustomerMO cust = new CustomerMO();
        cust.setFirstname(firstName);
        cust.setSurname(surName);

        cust.getPhones().add(phone1);
        cust.getPhones().add(phone2);
        em.persist(cust);

        em.persist(phone1);
        em.persist(phone2);

        return cust;
    }

    public CustomerMO updateCustomer(CustomerMO c) {
        return em.merge(c);
    }

    public PhoneMO createPhone(String type, String areacode, String phnumber) {
        PhoneMO phone1 = new PhoneMO();
        phone1.setNumber(phnumber);
        phone1.setAreacode(areacode);
        phone1.setType(type);
        em.persist(phone1);

        return phone1;
    }

    public PhoneMO updatePhone(PhoneMO p) {
        return em.merge(p);
    }

    public void deletePhone(PhoneMO p) {
        em.remove(em.merge(p));
    }

    public int retrieveOldPhoneListSizeFromCustomer(int id) {

        AuditReader reader = AuditReaderFactory.get(em);
        List<Number> revList = reader.getRevisions(CustomerMO.class, id);
        CustomerMO cust_rev = reader.find(CustomerMO.class, id, revList.get(revList.size() - 1));
        return cust_rev.getPhones().size();
    }

    public String retrieveOldPhoneListVersionFromCustomer(int id) {
        AuditReader reader = AuditReaderFactory.get(em);

        CustomerMO cust_rev = reader.find(CustomerMO.class, id, 1);
        return cust_rev.getPhones().get(0).getType();
    }

}
