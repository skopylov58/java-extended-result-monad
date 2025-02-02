package com.github.skopylov58.functional;

import lombok.*;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class XResult<T> {

    /**
     * Just marker interface for result error causes.
     */
    public interface ErrCause {
    }

    /**
     * Function that may throw an exception.
     *
     * @param <T> parameter type
     * @param <R> function result type
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * Predicate that may throw an exception.
     *
     * @param <T> parameter type
     */
    @FunctionalInterface
    public interface ThrowingPredicate<T> {
        boolean test(T t) throws Exception;
    }

    /**
     * Folds this XResult to R type.
     *
     * @param okMapper  mapper {@literal  T -> R} for Ok result
     * @param errMapper mapper {@literal ErrCause -> R} for Err result
     * @param <R>       mapper result type
     * @return mapper result
     */
    public abstract <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper);

    /**
     * Consumes this XResult.
     *
     * @param okConsumer  consumer for Ok result
     * @param errConsumer consumer for Err result.
     * @return current result
     */
    public abstract XResult<T> on(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer);

    /**
     * Gets XResult value.
     * @param defaultSupplier provides default value if this result is Err.
     * @return value or default value if this result is Err
     */
    public T getOrDefaut(Supplier<T> defaultSupplier) {
        return fold(ok -> ok, err -> defaultSupplier.get());
    }

    public <R> XResult<R> map(ThrowingFunction<? super T, ? extends R> mapper) {
        return fold(ok -> {
            try {
                return XResult.ofNullable(mapper.apply(ok));
            } catch (Exception e) {
                return XResult.err(e);
            }
        }, err -> (XResult<R>) this);
    }

    public <R> XResult<R> flatMap(ThrowingFunction<? super T, XResult<R>> mapper) {
        return fold(ok -> {
                    try {
                        XResult<R> applied = mapper.apply(ok);
                        return applied != null ? applied : err(new NullPointerException());
                    } catch (Exception e) {
                        return err(e);
                    }
                },
                err -> (XResult<R>) this
        );
    }

    /**
     * Filters this result.
     *
     * @param predicate             tester function
     * @param messageMapper maps value to string message to create FilteredCause if test fails.
     * @return this if test is successful, Err with FilteredCause otherwise or Err with ExceptionCause if exception happens.
     */
    public XResult<T> filter(ThrowingPredicate<? super T> predicate, Function<T, String> messageMapper) {
        return fold(ok -> {
                    try {
                        return predicate.test(ok) ? this : err(new FilterCause(messageMapper.apply(ok)));
                    } catch (Exception e) {
                        return err(e);
                    }
                },
                err -> this);
    }

    /**
     * Checks if given result is Ok.
     *
     * @return true if Ok
     */
    public boolean isOk() {
        return fold(t -> true, c -> false);
    }

    /**
     * Converts this result to Java Optional
     *
     * @return Java Optional.of() for Ok and Optional.empty() for Err result.
     */
    public Optional<T> optional() {
        return fold(Optional::ofNullable, c -> Optional.empty());
    }

    /**
     * Converts this result to Java Stream
     *
     * @return one element Java Stream for Ok and empty stream for Err.
     */
    public Stream<T> stream() {
        return fold(Stream::of, c -> Stream.empty());
    }

    /**
     * Factory method to create result from value of type T
     *
     * @param t   value, may be null
     * @param <T> result type
     * @return Ok result if t is not null, Err result with NullPointerException cause if t is null.
     */
    public static <T> XResult<T> ofNullable(T t) {
        return t != null ? new Ok<>(t) : err(new NullPointerException("XResult.ofNullable"));
    }

    /**
     * Factory method to create errors.
     *
     * @param cause any object that implements marker interface ErrCause
     * @param <T>   result type
     * @return Err result
     */
    public static <T> XResult<T> err(ErrCause cause) {
        return new Err<>(cause);
    }

    /**
     * Factor method to create errors from exceptions.
     *
     * @param e   exception
     * @param <T> result type
     * @return Err result
     */
    public static <T> XResult<T> err(Exception e) {
        return err(new ExceptionCause(e));
    }

    /**
     * Factory method to create SimpleCause errors.
     *
     * @param message err cause descriptions
     * @param <T>     result type
     * @return Err result
     */
    public static <T> XResult<T> err(String message) {
        return err(new SimpleCause(message));
    }

    /**
     * Ok result.
     *
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
        public XResult<T> on(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer) {
            okConsumer.accept(value);
            return this;
        }
    }

    /**
     * Err result
     *
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
        public XResult<T> on(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer) {
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

    /**
     * Filtered error cause, is used in filter() operation.
     */
    @Value
    public static class FilterCause implements ErrCause {
        String reason;
    }

    /**
     * Factory method to create results from Callable.
     *
     * @param callable callable that may throw an exception
     * @param <T>      result type
     * @return XResult
     */
    public static <T> XResult<T> fromCallable(Callable<? extends T> callable) {
        try {
            return XResult.ofNullable(callable.call());
        } catch (Exception e) {
            return XResult.err(e);
        }
    }
}