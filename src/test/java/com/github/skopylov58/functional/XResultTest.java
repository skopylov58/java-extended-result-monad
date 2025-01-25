package com.github.skopylov58.functional;

import org.junit.jupiter.api.Test;

import static com.github.skopylov58.functional.XResult.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.Closeable;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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
        ir.on(t -> fail(),
                err -> {
                    System.out.println(err);
                    assertInstanceOf(ExceptionCause.class, err);
                    if (err instanceof ExceptionCause) {
                        Exception npe = ((ExceptionCause) err).getException();
                        //System.getLogger("").log(System.Logger.Level.ERROR, "npe", npe);
                    }
                });
    }

    @Test
    void testResultInStream() {
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
    void testOptional() {
        XResult<Integer> xr = XResult.ok(12);
        Optional<Integer> op = xr.optional();
        assertTrue(op.isPresent());
        Integer i = op.get();
        assertEquals(Integer.valueOf(12), i);

        xr = XResult.err("not allowed");
        op = xr.optional();
        assertFalse(op.isPresent());
        try {
            i = op.get();
            fail();
        }catch (NoSuchElementException e) {
            //ok
        }
    }

    @Test
    void testStream() {
        XResult<Integer> xr = XResult.ok(12);
        Stream<Integer> is = xr.stream();
        List<Integer> list = is.collect(Collectors.toList());
        assertEquals(1, list.size());
        assertEquals(12, list.get(0));

        xr = XResult.err("not allowed");
        is = xr.stream();
        list = is.collect(Collectors.toList());
        assertTrue(list.isEmpty());
    }


    @Test
    void testCloseable() throws Exception {
        MyCloseable myCloseable = new MyCloseable();
        XResult<MyCloseable> result = ofNullable(myCloseable);
        try (Closeable c = result.asCloseable()) {
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
        foo.on(ok -> fail(),
                err -> {
                    assertInstanceOf(ExceptionCause.class, err);
                    System.out.println(err);
                });
    }

    URL createUrl(String s) throws Exception {
        return new URL(s);
    }
}