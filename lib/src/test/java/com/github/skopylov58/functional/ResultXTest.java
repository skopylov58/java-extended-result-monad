package com.github.skopylov58.functional;

import org.junit.jupiter.api.Test;

import static com.github.skopylov58.functional.ResultX.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

public class ResultXTest {

    @Test
    public void test() {

        ResultX<Integer> r = ResultX.ok(20);

        ResultX<Integer> rex = ResultX.err(new IOException());



    }


    @Test
    public void testNull() {
        ResultX<Integer> ir = ResultX.of(null);
        ir.consume(t -> fail(),
                e -> System.out.println(e));

    }

}