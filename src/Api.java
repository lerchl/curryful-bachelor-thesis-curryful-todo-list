import static io.github.curryful.rest.Server.listen;
import static io.github.curryful.rest.http.HttpContentType.APPLICATION_JSON;
import static io.github.curryful.rest.http.HttpResponseCode.OK;
import static io.github.curryful.rest.http.HttpResponseCode.NO_CONTENT;
import static io.github.curryful.rest.http.HttpResponseCode.NOT_FOUND;
import static io.github.curryful.rest.http.HttpResponseCode.BAD_REQUEST;
import static io.github.curryful.rest.http.HttpResponseCode.CREATED;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.curryful.commons.collections.ImmutableArrayList;
import io.github.curryful.commons.collections.MutableArrayList;
import io.github.curryful.commons.monads.Maybe;

import io.github.curryful.rest.Destination;
import io.github.curryful.rest.Endpoint;
import io.github.curryful.rest.RestFunction;
import io.github.curryful.rest.http.HttpMethod;
import io.github.curryful.rest.http.HttpResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Api {

    private static final AtomicInteger idGenerator = new AtomicInteger();
    private static final List<Todo> todos = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final RestFunction getTodos = context -> HttpResponse.of(OK, serialize(todos), APPLICATION_JSON);

    private static final RestFunction getTodoById = context -> findTodoById(
            context.getPathParameters().get("id").map(Integer::parseInt))
            .map(todo -> HttpResponse.of(OK, serialize(todo), APPLICATION_JSON))
            .orElse(HttpResponse.of(NOT_FOUND));

    private static final RestFunction putTodoById = context -> findTodoById(
            context.getPathParameters().get("id").map(Integer::parseInt))
            .flatMap(todo -> context.getBody().flatMap(body -> updateTodoFromBody(todo, body)))
            .map(todo -> HttpResponse.of(OK, serialize(todo), APPLICATION_JSON))
            .orElse(HttpResponse.of(NOT_FOUND));

    private static final RestFunction postTodo = context -> context.getBody()
            .flatMap(body -> deserialize(body, Todo.class))
            .map(todo -> {
                todo.setId(idGenerator.incrementAndGet());
                todos.add(todo);
                return HttpResponse.of(CREATED, serialize(todo), APPLICATION_JSON);
            }).orElse(HttpResponse.of(BAD_REQUEST));

    private static final RestFunction deleteTodoById = context -> findTodoById(
            context.getPathParameters().get("id").map(Integer::parseInt))
            .map(todo -> {
                todos.remove(todo);
                return HttpResponse.of(NO_CONTENT);
            })
            .orElse(HttpResponse.of(NOT_FOUND));

    private static final RestFunction postToggleTodoById = context -> findTodoById(
            context.getPathParameters().get("id").map(Integer::parseInt))
            .map(todo -> {
                todo.setCompleted(!todo.isCompleted());
                return HttpResponse.of(OK, serialize(todo), APPLICATION_JSON);
            })
            .orElse(HttpResponse.of(NOT_FOUND));

    private static Maybe<Todo> findTodoById(Maybe<Integer> id) {
        return id.flatMap(i -> Maybe.from(todos.stream().filter(todo -> todo.getId() == i).findFirst()));
    }

    private static Maybe<Todo> updateTodoFromBody(Todo existingTodo, String body) {
        return deserialize(body, Todo.class).map(newTodo -> {
            existingTodo.setTitle(newTodo.getTitle());
            existingTodo.setCompleted(newTodo.isCompleted());
            return existingTodo;
        });
    }

    private static String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> Maybe<T> deserialize(String content, Class<T> valueType) {
        try {
            return Maybe.just(objectMapper.readValue(content, valueType));
        } catch (Exception e) {
            return Maybe.none();
        }
    }

    public static class Todo {
        private int id;
        private String title;
        private boolean completed;

        public Todo() {
            // Jackson deserialization
        }

        public Todo(int id, String title, boolean completed) {
            this.id = id;
            this.title = title;
            this.completed = completed;
        }

        @JsonProperty
        public int getId() {
            return id;
        }

        @JsonProperty
        public String getTitle() {
            return title;
        }

        @JsonProperty
        public boolean isCompleted() {
            return completed;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

    public static void main(String[] args) {
        MutableArrayList<Endpoint> endpoints = MutableArrayList.empty();
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.GET, "/todos"), getTodos));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.POST, "/todos"), postTodo));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.GET, "/todos/:id"), getTodoById));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.PUT, "/todos/:id"), putTodoById));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.DELETE, "/todos/:id"), deleteTodoById));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.POST, "/todos/:id/toggle"), postToggleTodoById));

        var listenTry = listen.apply(ImmutableArrayList.empty()).apply(endpoints).apply(ImmutableArrayList.empty())
                .apply(8080);
        if (listenTry.isFailure()) {
            System.err.println("Server failed to start: " + listenTry.getError().getMessage());
            listenTry.getError().printStackTrace();
        }
    }
}
