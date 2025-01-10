package com.github.skopylov58.functional;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class ResultX<T> {

    interface ErrCause {}

    abstract <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper);

    abstract ResultX<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer);

    public boolean isOk() {
        return fold(t -> true, c -> false);
    }

    public boolean isErr() {
        return !isOk();
    }

    public <R> ResultX<R> map(Function<? super T, ? extends R> mapper) {
        return fold(t -> {
                    R r = mapper.apply(t);
                    if (r != null) {
                        return ok(r);
                    } else {
                        NullPointerException npe = new NullPointerException("map");
                        return err(new NullCause(npe));
                    }
                },
                c -> (ResultX<R>) this);
    }

    public <R> ResultX<R> flatMap(Function<? super T, ResultX<R>> mapper) {
        return fold(t -> {
                    ResultX<R> res = mapper.apply(t);
                    if (res != null) {
                        return res;
                    } else {
                        NullPointerException npe = new NullPointerException("flatMap");
                        return err(new NullCause(npe));
                    }
                },
                c -> (ResultX<R>) this);
    }

    public ResultX<T> filter(Predicate<? super T> predicate) {
        return fold(t -> predicate.test(t) ? this : err(new FilteredCause<>(t)),
                c -> this);
    }

    public Optional<T> optional() {
        return fold(Optional::ofNullable, c ->Optional.empty());
    }

    public Stream<T> stream() {
        return fold(Stream::of, c ->Stream.empty());
    }

    public static <T> ResultX<T> of(T t) {
        if (t != null) {
            return ok(t);
        } else {
            NullPointerException npe = new NullPointerException("of");
            return err(new NullCause(npe));
        }
    }

    public static <T> ResultX<T> ok(T t) {
        Objects.requireNonNull(t);
        return new Ok<>(t);
    }

    public static <T> ResultX<T> err(ErrCause cause) {
        return new Err<>(cause);
    }

    public static <T> ResultX<T> err(Exception e) {
        return new Err<>(new ExceptionCause(e));
    }

    static class Ok<T> extends ResultX<T> {
        final T value;

        Ok(T value) {
            this.value = value;
        }

        @Override
        <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper) {
            return okMapper.apply(value);
        }

        @Override
        ResultX<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer) {
            okConsumer.accept(value);
            return this;
        }
    }

    static class Err<T> extends ResultX<T> {
        final ErrCause cause;

        Err(ErrCause cause) {
            this.cause = cause;
        }

        @Override
        <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super ErrCause, ? extends R> errMapper) {
            return errMapper.apply(cause);
        }

        @Override
        ResultX<T> consume(Consumer<? super T> okConsumer, Consumer<? super ErrCause> errConsumer) {
            errConsumer.accept(cause);
            return this;
        }
    }

    public static class ExceptionCause implements ErrCause {
        final Exception ex;

        public ExceptionCause(Exception ex) {
            this.ex = ex;
        }
    }

    public static class NullCause implements ErrCause {
        final NullPointerException npe;

        public NullCause(NullPointerException npe) {
            this.npe = npe;
        }
    }

    public static class FilteredCause<T> implements ErrCause {
        final T filtered;

        public FilteredCause(T filtered) {
            this.filtered = filtered;
        }
    }
}