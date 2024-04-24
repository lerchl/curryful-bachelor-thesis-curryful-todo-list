package io.github.curryful.todo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Todo {
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
