package io.github.curryful.todo;

import static io.github.curryful.rest.Server.listen;
import static io.github.curryful.todo.Api.deleteTodoById;
import static io.github.curryful.todo.Api.getTodoById;
import static io.github.curryful.todo.Api.getTodos;
import static io.github.curryful.todo.Api.postTodo;
import static io.github.curryful.todo.Api.postToggleTodoById;
import static io.github.curryful.todo.Api.putTodoById;

import io.github.curryful.commons.collections.ImmutableArrayList;
import io.github.curryful.commons.collections.MutableArrayList;
import io.github.curryful.rest.Destination;
import io.github.curryful.rest.Endpoint;
import io.github.curryful.rest.http.HttpMethod;

public class Main {

    public static void main(String[] args) {
        MutableArrayList<Endpoint> endpoints = MutableArrayList.empty();
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.GET, "/todos"), getTodos));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.POST, "/todos"), postTodo));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.GET, "/todos/:id"), getTodoById));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.PUT, "/todos/:id"), putTodoById));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.DELETE, "/todos/:id"), deleteTodoById));
        endpoints.add(Endpoint.of(Destination.of(HttpMethod.POST, "/todos/:id/toggle"), postToggleTodoById));

        var listenTry = listen.apply(ImmutableArrayList.empty())
                .apply(endpoints)
                .apply(ImmutableArrayList.empty())
                .apply(8080);
        if (listenTry.isFailure()) {
            System.err.println("Server failed to start: " + listenTry.getError().getMessage());
            listenTry.getError().printStackTrace();
        }
    }
}
