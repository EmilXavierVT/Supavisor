package app.services.routeSecurity;

import app.dto.UserDTO;
import app.exceptions.TokenCreationException;
import app.exceptions.TokenVerificationException;

import java.text.ParseException;

public interface ITokenSecurity {
    UserDTO getUserWithRolesFromToken(String var1) throws ParseException;

    boolean tokenIsValid(String var1, String var2) throws ParseException, TokenVerificationException;

    boolean tokenNotExpired(String var1) throws ParseException;

    int timeToExpire(String var1) throws ParseException;

    String createToken(UserDTO var1, String var2, String var3, String var4) throws TokenCreationException;
}