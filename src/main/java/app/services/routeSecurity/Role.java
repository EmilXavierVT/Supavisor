package app.services.routeSecurity;

import io.javalin.security.RouteRole;

public enum Role implements RouteRole {
    ANYONE,USER,ADMIN,EMPLOYEE
}