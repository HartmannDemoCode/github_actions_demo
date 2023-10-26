package dk.cphbusiness.rest;

import dk.cphbusiness.security.SecurityRoutes;
import dk.cphbusiness.utils.Populate;

public class P08DataBaseDemo {

    public static void main(String[] args) {
        Populate populate = new Populate();
        populate.populate();
        ApplicationConfig
                .getInstance()
                .initiateServer()
                .setRoutes(SecurityRoutes.getSecurityRoutes())
                .setRoutes(SecurityRoutes.getSecuredRoutes())
                .startServer(7007)
                .checkSecurityRoles()
                .setGeneralExceptionHandling()
                .setErrorHandling()
                .setApiExceptionHandling();
    }
}
