# Pragmatic FP in Java

![example workflow](https://github.com/skopylov58/java-extended-result-monad/actions/workflows/gradle.yml/badge.svg)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/skopylov58/java-extended-result-monad/actions/workflows/gradle.yml)
[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://skopylov58.github.io/java-extended-result-monad/)
## Мотивация
Поводом к написанию этой статьи стал 
["Pragmatic FP" манифест](https://dzone.com/articles/introduction-to-pragmatic-functional-java)
в котором утверждается что прагматичное функциональное программирование (далее FP)
требует использования всего двух монад - `Option<T>` и `Result<T>`.

Данная статья в некотором смысле развивает идеи "Pragmatic FP" и утверждает что
прагматичное FP можно делать с помощью всего одной монады - `XResult<T>`

## Проблема

В функциональном программировании традиционно существует три монады для 
обработки/представления отказов (errors).
- `Option<T>/Maybe<T>` - для представления отсутствующего результата (null значение)
- `Result<T>/Try<T>` - для обработки случаев когда получение результата может завершиться исключением (exception)
- `Either<L,R>` - для всех остальных случае. Левое значение L традиционно ассоциируется с отказом,
правое R - c успешным результатом.

В Java Result и Either монады отсутствуют в составе стандартной библиотеки, Optional появился 
в Java8, однако его дизайн не идеален и часто критикуется, 
[для примера тут, в Oracle Java Magazine ](https://blogs.oracle.com/javamagazine/post/optional-class-null-pointer-drawbacks)

<details>
<summary>Что лично меня  не устраивает в Optional</summary>
Трудно установить место возниковения null при
использовании нескольких операций `map()` и непонятно какой фильтр сработал при использовании
нескольких операций `filter()`. Иными словами, Optional не сохраняет контекста при возникновении проблемы.
Ну да ладно, не будем слишком придираться.
</details>

Следующей общей проблемой в Java для всех монад является то, что разные монады плохо сочетаются (compose),
а точнее совсем не сочетаются при помощи стандартной для этого операции `flatMap()`. 
Для примера, попробуйте скомбинировать Optional и Result. Без использования `isPresent()`
и `isSuccess()` у вас это не получится, что делает использование FP в данном случае сомнительным. 

Далее, в реальной жизни возможностей Optional и Result часто бывает недостоточно. Рассмотрим
пример простой классической функции.

```java
public User getUserById(long id);
```

С точки зрения FP, данная функция всегда возвращает пользователя,
а на практике данная функция может
- вернуть пользователя User
- вернуть null
- выбросить Runtime исключение
- результат может зависеть от фазы Луны ;)

Представим себя на месте автора этой функции. Как сделать эту функцию безопасной? Так?

```java
public Optional<User> getUserById(long id);
```

Не пойдет, может быть выброшено исключение. Так?
```java
public Result<Optional<User>> getUserById(long id);
```

Нет, это уже слишком. Но и это еще не всё. Представим себе что мы получаем пользователя по REST API
и на HTTP запрос получили ответ с кодом из серии 400-500. То есть причиной сбоя (error cause) является 
не null значение и не исключение, а ошибка HTTP протокола. Как сделать функцию безопасной, что вернуть
в случае отказа? Ответ - монаду `XResult<T>`

## Что в имени тебе моём ...

X в имени `XResult<T>` означает
- eXtended - расширенный. XResult одновременно обладает свойствами `Option<T>`, `Result<T>`
и `Either<L,R>`
- eXtensible - расширяемый. Вы можете легко добавлять новые причины отказов (`ErrCause`) 
для вашей предметной области. (problem domain).

Ниже на диаграмме показан дизайн монады XResult. Он прост и традиционно имеет базовый класс XResult и 
двух потомков - `Ok`, который содержит результат типа `T` и `Err`, который содержит причину отказа 
`ErrCause`. `ErrCause` - маркерный интерфейс, который не накладывает никаких обязательств для
имплементирующих этот интерфейс. XResult предоставляет три имплементации ErrCause
- `ExceptionCause` - исключение 
- `FilterCause` - фильтрация с указанием возможной причины
- `SimpleCause` - причина сбоя которая описывается простой строкой.

Как мы видим, отсутствует `NullCause`, в котором нет особенной нужды. Вместо `NullCause` используется
`ExceptionCause` с исключением NullPointerException, которое создается в момент обнаружения null
значения (разумеется вместе с stack-trace, который позволяет обнаруживать место возникновения проблемы)

```mermaid
classDiagram
    XResult <|-- Ok
    XResult <|-- Err
    Err -- ErrCause
    
    ErrCause <|-- SimpleCause
    ErrCause <|-- ExceptionCause
    ErrCause <|-- FilterCause
    ErrCause <|-- HttpErrorCause

    note for ErrCause "Just marker interface, does not have any methods"

    class XResult~T~ {
        <<abstract>>
        abstract fold()
        abstract consume()
        map()
        flatMap()
        filter()
        ...()

    }
    
    class Ok ~T~ {
      T value
      fold()
      consume()
    }
    
    class Err{
      ErrCause cause
      fold()
      consume()
    }

    class ErrCause{
        <<interface>>
    }

    class SimpleCause{
        String message
    }

    class ExceptionCause {
        Exception exception
    }

    class FilterCause {
        String reason
    }

    class HttpErrorCause {
        String url
        String method
        int responseCode
        String responseMessage
    }
```

## XResult на практике

Попробуем решить указанную выше задачу получения пользователя при помощи XResult

```java
XResult<User> user = XResult.fromCallable(() -> getUserById(100));
```

Статическая функция `XResult.fromCallable()` обработает возможные исключения и нулевые результаты 
и вернет пользователя (или ошибку) упакованную в XResult. Далее с ним можно делать всё что угодно, 
как и с любой другой монадой - map(), flatMap(), filter() и др.

Теперь представим  себя на месте писателя этой функции. Чтобы сделать функцию безопасной
для применения, мы определим её следующим образом:

```java
public XResult<User> getUserById(long id) {
}
```

Из тела этой функции мы вернем
- `XResult.ofNullable(user)` - когда пользователь user получен по сети, null значение обработается автоматически
- `XResult.err(exception)` - когда мы перехватим любое исключение exception
- `XResult.err("HTTP error 401")` - в случае ошибок HTTP протокола

Давайте расширим обработку HTTP ошибок для того чтобы вызывающая сторона могла получить
больше информации об ошибке. Для этого определим класс HttpError

```java
public class HttpError implements ErrCause {
    int resultCode;
    String errorDescription;
    String method;
    String url;
    ...
}
```

Тогда при выявлении HTTP ошибки мы сделаем следующее

```java
    if (httpResponse.getCode() >= 400) {
        HttpError httpErr = new HttpError(httpResponse);
        return XRresult.err(httpErr);
    }
```

На вызывающнй стороне HttpError можно получить следующим образом:

```java
    XResult<User> user = getUserById(100);
    user.on( ok -> {
        // обработка успеха
        },
        err -> {
          if (err instanceof HttpError)) {
                HttpError httpErr = (HttpError) err;
                int code = httpErr.getResutCode();
          }
        })
```

## Реализация

XResult имплементирован в одном исходном файле 
(~320 строк кода с подробным javadoc) и не имеет run-time зависимостей,
[репозитарий на Github](https://github.com/skopylov58/java-extended-result-monad)

Для компиляции используется Java версии 8 (`JavaLanguageVersion.of(8)`), 
это сделано преднамеренно, потому что может возникнуть необходимость 
применить XResult для древнего легаси.

При использовании Java 17+ код XResult может быть легко переписан с использованием
запечатанных (sealed) интерфейсов, записей (records), сопоставления с образцом 
(pattern matching), что сделает код еще более коротким и удобным в использовании.
Но это на будущее.












