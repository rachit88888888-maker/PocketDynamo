package org.db;

public  enum Command {
    SET("Store a value"),
    GET("Retrieve a value"),
    DEL("Delete a value"),
    EXIT("Gracefully shutdown");

    private final String description;

    Command(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }


}