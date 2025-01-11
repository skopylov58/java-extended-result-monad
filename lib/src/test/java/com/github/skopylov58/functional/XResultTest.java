package com.github.skopylov58.functional;

import org.junit.jupiter.api.Test;

import static com.github.skopylov58.functional.XResult.*;
import static org.junit.jupiter.api.Assertions.fail;

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
        XResult<Integer> ir = XResult.of(null);
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

        List<XResult<Integer>> collected = Stream.of(1, 2, 3, null, 0, -5)
                .map(XResult::of)
                .map(xr -> xr.filter(i -> i % 2 == 0))
                .collect(Collectors.toList());

        collected.forEach(
                xr -> System.out.println(xr.toString())
        );


    }
}