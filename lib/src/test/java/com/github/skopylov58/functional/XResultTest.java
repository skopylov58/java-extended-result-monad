package com.github.skopylov58.functional;

import org.junit.jupiter.api.Test;

import static com.github.skopylov58.functional.XResult.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XResultTest {

    @Test
    public void testOk() {
        XResult<Integer> ok = XResult.ok(20);
        assertTrue(ok.isOk());

        try {
            XResult<Integer> err = XResult.<Integer>ok(null);
            fail();
        } catch (NullPointerException e) {
            //ok
            //Does not allow nulls in XResult
        }
    }


    @Test
    public void testNull() {
        XResult<Integer> ir = XResult.ofNullable(null);
        ir.consume(t -> fail(),
                err -> {
                    System.out.println(err);
                    assertInstanceOf(ExceptionCause.class, err);
                    if (err instanceof ExceptionCause) {
                        Exception npe = ((ExceptionCause) err).getException();
                        System.getLogger("").log(System.Logger.Level.ERROR, "npe", npe);
                    }
                });
    }

    @Test
    void testStream() {
        Stream.of(1, 2, 3, null, 0, -5)
                .map(XResult::ofNullable)
                .map(xr -> xr.filter(i -> i % 2 == 0, i -> i + " Not even"))
                .forEach(xr -> System.out.println(xr.toString()));
    }

    @Test
    void isOk() {
        XResult<Integer> r1 = XResult.ofNullable(10);
        assertInstanceOf(Ok.class, r1);
        assertTrue(r1.isOk());
        assertFalse(r1.isErr());
    }

    @Test
    void testCloseable() throws Exception {
        MyCloseable myCloseable = new MyCloseable();
        XResult<MyCloseable> result = ofNullable(myCloseable);
        try (var c = result.asCloseable()) {
            assertTrue(result.isOk());
            XResult<Integer> i = result.map(clo -> 1);
        }
        assertTrue(myCloseable.closed);
    }

    static class MyCloseable implements Closeable {
        boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    void testSafeMapper() {
        XResult<URL> foo = ok("foo").map(this::createUrl);
        assertTrue(foo.isErr());
        System.out.println(foo);

        XResult<URL> google = ok("http://www.google.com").map(this::createUrl);
        assertTrue(google.isOk());
        System.out.println(google);

    }

    @Test
    void testSafeMapperShort() {
        XResult<URL> foo = ok("foo").map(URL::new);
        assertTrue(foo.isErr());
        foo.consume(ok -> fail(),
                err -> {
                    assertInstanceOf(ExceptionCause.class, err);
                    System.out.println(err);
                });
    }

    URL createUrl(String s) throws Exception {
        return new URL(s);
    }
}