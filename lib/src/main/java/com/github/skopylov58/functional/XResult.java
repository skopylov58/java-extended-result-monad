package com.github.skopylov58.functional;

import lombok.*;

import java.io.Closeable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class XResult<T> {

    /**
     * Just marker interface for result error causes.
     */
    public interface ErrCause {}

    /**
     * Function that may throw an exception.
     * @param <T> parameter type
     * @param <R> function result type
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * Folds this XResult to R type.
     *
     * @param okMapper mapper {@literal  T -> R} for Ok result
     * @param errMapper mapper {@literal ErrCause -> R} for Err result
     * @return mapper result
     * @param <R> mapper result type
     */
    public abstract <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper);

    /**
     * Consumes this XResult.
     * @param okConsumer consumer for Ok result
     * @param errConsumer consumer for Err result.
     * @return current result
     */
    public abstract XResult<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer);

    /**
     * Checks if given result is Ok.
     * @return true if Ok
     */
    public boolean isOk() {
        return fold(t -> true, c -> false);
    }

    /**
     * Checks if given result is Err
     * @return true if Err
     */
    public boolean isErr() {
        return !isOk();
    }

    public <R> XResult<R> map(Function<? super T, ? extends R> mapper) {
        return fold(t -> ofNullable(mapper.apply(t)),c -> (XResult<R>) this);
    }

    public <R> XResult<R> flatMap(Function<? super T, XResult<R>> mapper) {
        return fold(t -> Objects.requireNonNull(mapper.apply(t)),c -> (XResult<R>) this);
    }

    /**
     * Filters this result.
     * @param predicate tester function
     * @return this if test is successful, Err with FILTERED_NO_REASON cause otherwise.
     */
    public XResult<T> filter(Predicate<? super T> predicate) {
        return fold(t -> predicate.test(t) ? this : err(FILTERED_NO_REASON),c -> this);
    }

    /**
     * Filters this result.
     * @param predicate tester function
     * @param filteredMessageMapper maps value to string message to create FilteredCause if test fails.
     * @return this if test is successful, Err with FilteredCause otherwise.
     */
    public XResult<T> filter(Predicate<? super T> predicate, Function<T, String> filteredMessageMapper) {
        return fold(t -> predicate.test(t) ? this : err(new FilteredCause(filteredMessageMapper.apply(t))),c -> this);
    }

    /**
     * Converts this result to Java Optional
     * @return Java Optional.of() for Ok and Optional.empty() for Err result.
     */
    public Optional<T> optional() {
        return fold(Optional::ofNullable, c ->Optional.empty());
    }

    /**
     * Converts this result to Java Stream
     * @return one element Java Stream for Ok and empty stream for Err.
     */
    public Stream<T> stream() {
        return fold(Stream::of, c ->Stream.empty());
    }

    public Closeable asCloseable() {
        return fold ( t -> () -> {
            if (t instanceof Closeable) {
                Closeable clo = (Closeable) t;
                clo.close();
            }
        },
                 //Should errors to be closeable too?
        cause -> () -> {}
        );
    }

    /**
     * Factory method to create result from value of type T
     * @param t value, may be null
     * @return Ok result if t is not null, Err result with NullPointerException cause if t is null.
     * @param <T> result type
     */
    public static <T> XResult<T> ofNullable(T t) {
        return t != null ? ok(t) : err(new NullPointerException("XResult.ofNullable"));
    }

    /**
     * Factory method to create result from value of type T
     * @param t value, may not be null
     * @return Ok result if t is not null.
     * @throws NullPointerException if t is null.
     * @param <T> result type
     */
    public static <T> XResult<T> ok(@NonNull T t) {
        return new Ok<>(Objects.requireNonNull(t));
    }

    /**
     * Factory method to create errors.
     * @param cause any object that implements marker interface ErrCause
     * @return Err result
     * @param <T> result type
     */
    public static <T> XResult<T> err(ErrCause cause) {
        return new Err<>(cause);
    }

    /**
     * Factor method to create errors from exceptions.
     * @param e exception
     * @return Err result
     * @param <T> result type
     */
    public static <T> XResult<T> err(Exception e) {
        return err(new ExceptionCause(e));
    }

    /**
     * Factory method to create SimpleCause errors.
     * @param message err cause descriptions
     * @return Err result
     * @param <T> result type
     */
    public static <T> XResult<T> err(String message) {
        return err(new SimpleCause(message));
    }

    /**
     * Ok result.
     * @param <T> result type
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @ToString
    public static class Ok<T> extends XResult<T> {
        private final T value;

        @Override
        public <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper) {
            return okMapper.apply(value);
        }

        @Override
        public XResult<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer) {
            okConsumer.accept(value);
            return this;
        }
    }

    /**
     * Err result
     * @param <T> result type
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @ToString
    public static class Err<T> extends XResult<T> {
        final private ErrCause cause;

        @Override
        public <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper) {
            return errMapper.apply(cause);
        }

        @Override
        public XResult<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer) {
            errConsumer.accept(cause);
            return this;
        }
    }

    /**
     * Simple error cause.
     */
    @Value
    public static class SimpleCause implements ErrCause {
        String message;
    }

    /**
     * Exception error cause.
     */
    @Value
    public static class ExceptionCause implements ErrCause {
        Exception exception;
    }

    public static final FilteredCause FILTERED_NO_REASON = new FilteredCause("");

    /**
     * Filtered error cause, is used in filter() operation.
     */
    @Value
    public static class FilteredCause implements ErrCause {
        String reason;
    }


    /**
     * Helper function to transform throwing partial function to error safe one.
     * @param function that may throw Exception
     * @return function that returns XResult
     * @param <T> function argument type
     * @param <R> function result type
     */
    public static <T, R> Function<? super T, XResult<R>> safeMapper(ThrowingFunction<? super T, ? extends R> function) {
        return t -> {
            try {
                return XResult.ofNullable(function.apply(t));
            } catch (Exception e) {
                return XResult.err(e);
            }
        };
    }

    /**
     * Factory method to create results from Callable.
     * @param callable callable that may throw an exception
     * @return XResult
     * @param <T> result type
     */
    public static <T> XResult<T> fromCallable(Callable<? extends T> callable) {
        try {
            return XResult.ofNullable(callable.call());
        } catch (Exception e) {
            return XResult.err(e);
        }
    }
}