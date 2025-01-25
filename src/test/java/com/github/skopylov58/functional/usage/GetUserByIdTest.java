package com.github.skopylov58.functional.usage;

import com.github.skopylov58.functional.XResult;
import com.github.skopylov58.functional.XResult.ExceptionCause;
import com.github.skopylov58.functional.XResult.SimpleCause;
import lombok.Data;
import lombok.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GetUserByIdTest {

    @Value
    public static class User {
        int age;
        String name;
    }

    @Value
    public static class HttpError implements XResult.ErrCause {
        int responseCode;
        String message;
        String method;
        String url;
    }

    User getUserById(int id) {
        switch (id) {
            case 0:
                return new User(20, "Andy");
            case 1:
                throw new IllegalArgumentException("Fail get user");
            default:
                return null;
        }
    }

    @Test
    void testGetUser() {

        XResult<User> user = XResult.fromCallable(() -> getUserById(0));
        assertTrue(user.isOk());
        user.on(ok -> assertEquals("Andy", ok.getName()),
                err -> fail()
        );

        user = XResult.fromCallable(() -> getUserById(1));
        assertFalse(user.isOk());
        user.on(ok -> fail(),
                err -> {
                    ExceptionCause ex = (ExceptionCause) err;
                    assertInstanceOf(IllegalArgumentException.class, ex.getException());
                }
        );

        user = XResult.fromCallable(() -> getUserById(2));
        assertFalse(user.isOk());
        user.on(ok -> fail(),
                err -> {
                    ExceptionCause ex = (ExceptionCause) err;
                    assertInstanceOf(NullPointerException.class, ex.getException());
                }
        );
    }

    XResult<User> getUserResultById(int id) {
        switch (id) {
            case 0:
                return XResult.ofNullable(new User(20, "Andy"));
            case 1:
                return XResult.err(new IllegalArgumentException("Fail get user"));
            case 2:
                return XResult.err("HTTP 404");
            case 3:
                HttpError httpError = new HttpError(401, "message", "GET", "url");
                return XResult.err(httpError);
            default:
                return XResult.ofNullable(null);
        }
    }


    @Test
    void testGetUserResult() {

        XResult<User> user = getUserResultById(0);
        assertTrue(user.isOk());
        user.on(ok -> assertEquals("Andy", ok.getName()),
                err -> fail()
        );

        user = getUserResultById(1);
        assertFalse(user.isOk());
        user.on(ok -> fail(),
                err -> {
                    ExceptionCause ex = (ExceptionCause) err;
                    assertInstanceOf(IllegalArgumentException.class, ex.getException());
                }
        );

        user = getUserResultById(2);
        assertFalse(user.isOk());
        user.on(ok -> fail(),
                err -> {
                    SimpleCause simple = (SimpleCause) err;
                    assertEquals("HTTP 404", simple.getMessage());
                }
        );

        user = getUserResultById(3);
        assertFalse(user.isOk());
        user.on(ok -> fail(),
                err -> {
                    HttpError httpErr = (HttpError) err;
                    assertEquals("GET", httpErr.getMethod());
                }
        );

        user = getUserResultById(4);
        assertFalse(user.isOk());
        user.on(ok -> fail(),
                err -> {
                    ExceptionCause ex = (ExceptionCause) err;
                    assertInstanceOf(NullPointerException.class, ex.getException());
                }
        );
    }
}
