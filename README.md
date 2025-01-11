# java-extended-result-monad
Extended result monad for java

```mermaid
classDiagram
    XResult <|-- Ok
    XResult <|-- Err
    Err -- ErrCause
    
    ErrCause <|-- SimpleCause
    ErrCause <|-- ExceptionCause
    ErrCause <|-- FilteredCause
    ErrCause <|-- HttpResponseCause

    class XResult{
        abstract fold()
        abstract consume()
        map()
        flatMap()
        filter()

    }
    
    class Ok{
      T value
    }
    
    class Err{
      ErrCause cause
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

    class FilteredCause {
        String filtered
    }

    class HttpResponseCause {
        String url
        String method
        int responseCode
        String responseMessage
    }
```