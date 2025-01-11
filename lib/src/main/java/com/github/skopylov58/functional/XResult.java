package com.github.skopylov58.functional;

import lombok.*;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class XResult<T> {

    interface ErrCause {}

    abstract <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper);

    abstract XResult<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer);

    public boolean isOk() {
        return fold(t -> true, c -> false);
    }

    public boolean isErr() {
        return !isOk();
    }

    public <R> XResult<R> map(Function<? super T, ? extends R> mapper) {
        return fold(t -> {
                    R r = mapper.apply(t);
                    if (r != null) {
                        return ok(r);
                    } else {
                        NullPointerException npe = new NullPointerException("XResult#map");
                        return err(npe);
                    }
                },
                c -> (XResult<R>) this);
    }

    public <R> XResult<R> flatMap(Function<? super T, XResult<R>> mapper) {
        return fold(t -> {
                    XResult<R> res = mapper.apply(t);
                    if (res != null) {
                        return res;
                    } else {
                        NullPointerException npe = new NullPointerException("XResult#flatMap");
                        return err(npe);
                    }
                },
                c -> (XResult<R>) this);
    }

    public XResult<T> filter(Predicate<? super T> predicate) {
        return fold(t -> predicate.test(t) ? this : err(new FilteredCause(t.toString())),
                c -> this);
    }

    public Optional<T> optional() {
        return fold(Optional::ofNullable, c ->Optional.empty());
    }

    public Stream<T> stream() {
        return fold(Stream::of, c ->Stream.empty());
    }

    public static <T> XResult<T> of(T t) {
        if (t != null) {
            return ok(t);
        } else {
            NullPointerException npe = new NullPointerException("XResult#of");
            return err(npe);
        }
    }

    public static <T> XResult<T> ok(@NonNull T t) {
        Objects.requireNonNull(t);
        return new Ok<>(t);
    }

    public static <T> XResult<T> err(ErrCause cause) {
        return new Err<>(cause);
    }

    public static <T> XResult<T> err(Exception e) {
        return new Err<>(new ExceptionCause(e));
    }

    public static <T> XResult<T> err(String message) {
        return new Err<>(new SimpleCause(message));
    }

    @RequiredArgsConstructor
    @ToString
    static class Ok<T> extends XResult<T> {
        private final T value;

        @Override
        <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper) {
            return okMapper.apply(value);
        }

        @Override
        XResult<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer) {
            okConsumer.accept(value);
            return this;
        }
    }

    @RequiredArgsConstructor
    @ToString
    static class Err<T> extends XResult<T> {
        final private ErrCause cause;

        @Override
        <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper) {
            return errMapper.apply(cause);
        }

        @Override
        XResult<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer) {
            errConsumer.accept(cause);
            return this;
        }
    }

    @Value
    public static class SimpleCause implements ErrCause {
        String message;
    }

    @Value
    public static class ExceptionCause implements ErrCause {
        Exception exception;
    }

    @Value
    public static class FilteredCause implements ErrCause {
        String filtered;
    }
}