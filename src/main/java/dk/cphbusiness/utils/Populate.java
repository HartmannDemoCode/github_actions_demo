package dk.cphbusiness.utils;

import dk.cphbusiness.data.HibernateConfig;
import dk.cphbusiness.security.ISecurityDAO;
import dk.cphbusiness.security.Role;
import dk.cphbusiness.security.SecurityDAO;
import dk.cphbusiness.security.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class Populate {
    ISecurityDAO securityDAO = SecurityDAO.getInstance(HibernateConfig.getEntityManagerFactory());

    public static void main(String[] args) {
        Populate populate = new Populate();
        populate.populate();
    }

    public void populate() {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        try(EntityManager em = emf.createEntityManager()){
            em.getTransaction().begin();
            em.createQuery("DELETE FROM User").executeUpdate();
            em.createQuery("DELETE FROM Role").executeUpdate();
            em.getTransaction().commit();
        }
        securityDAO.createUser("user1", "test123");
        securityDAO.createUser("admin1", "test123");
        Role adminRole = securityDAO.createRole("admin");
        securityDAO.addUserRole("user1", "admin");
        securityDAO.removeUserRole("admin1", "user");
    }
}
