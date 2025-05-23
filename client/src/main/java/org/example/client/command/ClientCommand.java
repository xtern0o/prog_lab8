package org.example.client.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@AllArgsConstructor
@Getter
public abstract class ClientCommand implements ClientCommandInterface{
    private final String name;
    private final String description;

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }

    @Override
    public String toString() {
        return String.format(": %-30s | %s", name, description);
    }
}
