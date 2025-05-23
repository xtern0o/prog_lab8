package org.example.common.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.common.dtp.User;
import org.example.common.utils.Validatable;

/**
 * Модель Ticket
 * @author maxkarn
 */
@Getter
@Setter
@AllArgsConstructor
public class Ticket implements Validatable, Comparable<Ticket>, Serializable {
    private static final long serialVersionUID = 6476823748348364237L;

    private Integer id; //Поле не может быть null, Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически

    private String name; //Поле не может быть null, Строка не может быть пустой

    private Coordinates coordinates; //Поле не может быть null

    private ZonedDateTime creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически

    private double price; //Значение поля должно быть больше 0

    private Float discount; //Поле не может быть null, Значение поля должно быть больше 0, Максимальное значение поля: 100

    private boolean refundable;

    private TicketType type; //Поле может быть null

    private Person person; //Поле не может быть null

    private String ownerLogin;

    public Ticket(String name, Coordinates coordinates, double price, Float discount, TicketType type, boolean refundable, Person person, String ownerLogin) {
        this.name = name;
        this.coordinates = coordinates;
        this.price = price;
        this.discount = discount;
        this.type = type;
        this.refundable = refundable;
        this.person = person;
        this.creationDate = ZonedDateTime.now();
        this.ownerLogin = ownerLogin;

        this.id = 0;
    }

    @Override
    public boolean validate() {
        if (name == null || name.isEmpty()) return false;
        if (creationDate == null) return false;
        if (price <= 0) return false;
        if (discount == null || discount <= 0 || discount > 100) return false;
        if (person == null) return false;

        return person.validate() && coordinates.validate();
    }

    @Override
    public String toString() {
        return String.format(
                "Ticket         [owner: %s]\n" +
                ": id           | %d\n" +
                ": name         | %s\n" +
                ": coordinates  | %s\n" +
                ": creationDate | %s\n" +
                ": price        | %.2f\n" +
                ": discount     | %.2f\n" +
                ": refundable   | %s\n" +
                ": type         | %s\n" +
                ": person       | %s",
                ownerLogin,
                id,
                name,
                coordinates,
                this.creationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                price,
                discount,
                refundable ? "Yes" : "No",
                type != null ? type.name() : "?",
                person
        );
    }

    @Override
    public int compareTo(Ticket ticket) {
        return this.coordinates.compareTo(ticket.coordinates);
    }
}