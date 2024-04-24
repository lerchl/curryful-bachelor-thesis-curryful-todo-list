package io.github.curryful.todo;

import static io.github.curryful.rest.http.HttpContentType.APPLICATION_JSON;
import static io.github.curryful.rest.http.HttpResponseCode.BAD_REQUEST;
import static io.github.curryful.rest.http.HttpResponseCode.CREATED;
import static io.github.curryful.rest.http.HttpResponseCode.NOT_FOUND;
import static io.github.curryful.rest.http.HttpResponseCode.NO_CONTENT;
import static io.github.curryful.rest.http.HttpResponseCode.OK;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.curryful.commons.monads.Maybe;
import io.github.curryful.rest.RestFunction;
import io.github.curryful.rest.http.HttpResponse;

public class Api {

    private static final AtomicInteger idGenerator = new AtomicInteger();
    private static final List<Todo> todos = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final RestFunction getTodos = context -> HttpResponse.of(OK, serialize(todos), APPLICATION_JSON);

    public static final RestFunction getTodoById = context -> findTodoById(
            context.getPathParameters().get("id").map(Integer::parseInt))
            .map(todo -> HttpResponse.of(OK, serialize(todo), APPLICATION_JSON))
            .orElse(HttpResponse.of(NOT_FOUND));

    public static final RestFunction putTodoById = context -> findTodoById(
            context.getPathParameters().get("id").map(Integer::parseInt))
            .flatMap(todo -> context.getBody().flatMap(body -> updateTodoFromBody(todo, body)))
            .map(todo -> HttpResponse.of(OK, serialize(todo), APPLICATION_JSON))
            .orElse(HttpResponse.of(NOT_FOUND));

    public static final RestFunction postTodo = context -> context.getBody()
            .flatMap(body -> deserialize(body, Todo.class))
            .map(todo -> {
                todo.setId(idGenerator.incrementAndGet());
                todos.add(todo);
                return HttpResponse.of(CREATED, serialize(todo), APPLICATION_JSON);
            }).orElse(HttpResponse.of(BAD_REQUEST));

    public static final RestFunction deleteTodoById = context -> findTodoById(
            context.getPathParameters().get("id").map(Integer::parseInt))
            .map(todo -> {
                todos.remove(todo);
                return HttpResponse.of(NO_CONTENT);
            })
            .orElse(HttpResponse.of(NOT_FOUND));

    public static final RestFunction postToggleTodoById = context -> findTodoById(
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
}
