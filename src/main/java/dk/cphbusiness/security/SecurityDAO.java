package dk.cphbusiness.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dk.cphbusiness.exceptions.ApiException;
import dk.cphbusiness.security.exceptions.NotAuthorizedException;
import dk.cphbusiness.security.exceptions.ValidationException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;


public class SecurityDAO implements ISecurityDAO {

    private static ISecurityDAO instance;
    private static EntityManagerFactory emf;

    private SecurityDAO() {
    }

    public static ISecurityDAO getInstance(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new SecurityDAO();
        }
        return instance;
    }

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    @Override
    public User getVerifiedUser(String username, String password) throws ValidationException {
        try (EntityManager em = getEntityManager()) {
            User user = em.find(User.class, username);
            if (user == null)
                throw new EntityNotFoundException("No user found with username: " + username); //RuntimeException
            user.getRoles().size(); // force roles to be fetched from db
            if (!user.verifyPassword(password))
                throw new ValidationException("Wrong password");
            return user;
        }
    }

    @Override
    public Role createRole(String role) {
        try (EntityManager em = getEntityManager()) {
            Role roleEntity = em.find(Role.class, role);
            if (roleEntity != null)
                return roleEntity;

            roleEntity = new Role(role);
            em.getTransaction().begin();
            em.persist(roleEntity);
            em.getTransaction().commit();
            return roleEntity;
        }
    }

    @Override
    public User createUser(String username, String password) {
        try (EntityManager em = getEntityManager()) {
            User userEntity = em.find(User.class, username);
            if (userEntity != null)
                throw new EntityExistsException("User with username: " + username + " already exists");
            userEntity = new User(username, password);
            em.getTransaction().begin();
            Role userRole = em.find(Role.class, "user");
            if (userRole == null)
                em.persist(userRole = new Role("user"));
            userEntity.addRole(userRole);
            em.persist(userEntity);
            em.getTransaction().commit();
            return userEntity;
        }
    }

    @Override
    public User getUser(String userName) {
        try (EntityManager em = getEntityManager()) {
            User user = em.createQuery("SELECT u FROM User u JOIN u.roles WHERE u.username = :username", User.class).setParameter("username", userName).getSingleResult();
            if (user == null)
                throw new EntityNotFoundException("No user found with username: " + userName);
            user.getRoles().size();
            return user;
        }
    }

    @Override
    public User addUserRole(String username, String role) {
        try (EntityManager em = getEntityManager()) {
            User user = em.find(User.class, username);
            if (user == null)
                throw new EntityNotFoundException("No user found with username: " + username);
            Role roleEntity = em.find(Role.class, role);
            if (roleEntity == null)
                throw new EntityNotFoundException("No role found with name: " + role);
            if (user.getRoles().contains(roleEntity))
                return user;
            em.getTransaction().begin();
            user.getRoles().add(roleEntity);
            em.getTransaction().commit();
            return user;
        }
    }

    @Override
    public User removeUserRole(String username, String role) {
        try (EntityManager em = getEntityManager()) {
            User user = em.find(User.class, username);
            if (user == null)
                throw new EntityNotFoundException("No user found with username: " + username);
            Role roleEntity = em.find(Role.class, role);
            if (roleEntity == null)
                throw new EntityNotFoundException("No role found with name: " + role);
            if (!user.getRoles().contains(roleEntity))
                return user;
            em.getTransaction().begin();
            user.getRoles().remove(roleEntity);
            em.getTransaction().commit();
            return user;
        }
    }

    @Override
    public boolean hasRole(String role, User userEntity) {
        try (EntityManager em = getEntityManager()) {
            Role roleEntity = em.find(Role.class, role);
            if (roleEntity == null)
                throw new EntityNotFoundException("No role found with name: " + role);
            return userEntity.getRoles().contains(roleEntity);
        }
    }

}
