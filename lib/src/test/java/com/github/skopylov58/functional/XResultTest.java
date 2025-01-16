package com.github.skopylov58.functional;

import org.junit.jupiter.api.Test;

import static com.github.skopylov58.functional.XResult.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XResultTest {

    @Test
    public void test() {

        XResult<Integer> r = XResult.ok(20);

        XResult<Integer> rex = XResult.err(new IOException());


    }


    @Test
    public void testNull() {
        XResult<Integer> ir = XResult.ofNullable(null);
        ir.consume(t -> fail(),
                err -> {
                    System.out.println(err);
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

}