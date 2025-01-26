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
        XResult<Integer> ok = XResult.ofNullable(20);
        assertTrue(ok.isOk());

        XResult<Integer> err = XResult.<Integer>ofNullable(null);
        assertFalse(err.isOk());
    }


    @Test
    public void testOfNullable() {
        XResult<Integer> ir = XResult.ofNullable(null);
        ir.on(ok -> fail(),
                err -> {
                    System.out.println(err);
                    assertInstanceOf(ExceptionCause.class, err);
                    Exception npe = ((ExceptionCause) err).getException();
                    //System.getLogger("").log(System.Logger.Level.ERROR, "npe", npe);
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
        assertTrue(r1.isOk());
    }

    @Test
    void testOptional() {
        XResult<Integer> xr = XResult.ofNullable(12);
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
        } catch (NoSuchElementException e) {
            //ok
        }
    }

    @Test
    void testStream() {
        XResult<Integer> xr = XResult.ofNullable(12);
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
    void testMap() {
        XResult<URL> foo = ofNullable("foo").map(this::createUrl);
        assertFalse(foo.isOk());
        System.out.println(foo);

        XResult<String> host = foo.map(URL::getHost);
        assertFalse(foo.isOk());

        XResult<URL> google = ofNullable("http://www.google.com").map(this::createUrl);
        assertTrue(google.isOk());
        System.out.println(google);
    }

    @Test
    void testMapShort() {
        XResult<URL> foo = ofNullable("foo").map(URL::new);
        assertFalse(foo.isOk());
        foo.on(ok -> fail(),
                err -> {
                    assertInstanceOf(ExceptionCause.class, err);
                    System.out.println(err);
                });
    }

    @Test
    void testOkFlatMap() {

        XResult<Integer> ir = XResult.ofNullable(12);
        XResult<String> sr = XResult.ofNullable("Twelve");

        XResult<String> mapped = ir.flatMap(i -> sr.map(s -> s + i));
        assertTrue(mapped.isOk());

        mapped.on(ok -> assertEquals("Twelve12", ok), err -> fail());

        String folded = mapped.fold(i -> i, null);
        assertEquals("Twelve12", folded);
    }

    @Test
    void testErrFlatMap() {

        XResult<Integer> ir = XResult.ofNullable(12);
        XResult<String> sr = XResult.err("Twelve");

        XResult<String> mapped = sr.flatMap(s -> ir.map(i -> s + i));

        mapped.on(ok -> fail(), err -> assertInstanceOf(SimpleCause.class, err));

        assertFalse(mapped.isOk());
        String folded = mapped.fold(ok -> ok, err -> null);
        assertNull(folded);
    }

    @Test
    void testThrowingFlatMap() {

        XResult<Integer> ir = XResult.ofNullable(12);

        XResult<String> mapped = ir.flatMap(i -> {
            throw new IllegalArgumentException();
        });

        mapped.on(ok -> fail(), err -> assertInstanceOf(ExceptionCause.class, err));

        assertFalse(mapped.isOk());
        String folded = mapped.fold(ok -> ok, err -> null);
        assertNull(folded);
    }

    @Test
    void testNullFlatMap() {

        XResult<Integer> ir = XResult.ofNullable(12);

        XResult<String> mapped = ir.flatMap(i -> null);

        mapped.on(ok -> fail(),
                err -> {
                    assertInstanceOf(ExceptionCause.class, err);
                    ExceptionCause ec = (ExceptionCause) err;
                    assertInstanceOf(NullPointerException.class, ec.getException());
                });

        assertFalse(mapped.isOk());
        String folded = mapped.fold(ok -> ok, err -> null);
        assertNull(folded);
    }


    @Test
    void testFilter() {
        XResult<Integer> ir = ofNullable(12);
        ir = ir.filter(i -> i%2==0, i -> i + " is odd");
        ir.on(ok-> assertEquals(12, ok), err -> fail());
        System.out.println(ir);

        ir = ofNullable(13);
        ir = ir.filter(i -> i%2==0, i -> i + " is odd");
        ir.on(ok-> fail(), err -> assertInstanceOf(FilterCause.class, err));
        System.out.println(ir);

        ir = ofNullable(13);
        ir = ir.filter(i -> {throw new IllegalArgumentException();}, i -> i + " is odd");
        ir.on(ok-> fail(), err -> assertInstanceOf(ExceptionCause.class, err));
        System.out.println(ir);

    }

    @Test
    void testGetOrDefault() {

        XResult<Integer> i = XResult.ofNullable(12);
        Integer got = i.getOrDefaut(() -> 13);
        assertEquals(12, got);

        i = XResult.ofNullable(null);
        got = i.getOrDefaut(() -> 13);
        assertEquals(13, got);
    }

    URL createUrl(String s) throws Exception {
        return new URL(s);
    }
}