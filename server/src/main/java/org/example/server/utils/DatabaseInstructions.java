package org.example.server.utils;


/**
 * Датакласс с командами PSQL
 */
public class DatabaseInstructions {
    public static String createDatabase =
            """
            CREATE TYPE COUNTRY AS ENUM (
                'FRANCE',
                'CHINA',
                'ITALY'
            );
            CREATE TYPE TICKET_TYPE AS ENUM (
                'VIP',
                'USUAL',
                'BUDGETARY',
                'CHEAP'
            );
            CREATE TABLE IF NOT EXISTS users (
                id SERIAL PRIMARY KEY,
                login VARCHAR(255) UNIQUE NOT NULL CHECK(length(login) > 3),
                password TEXT NOT NULL,
                salt TEXT
            );
            CREATE TABLE IF NOT EXISTS tickets (
                id SERIAL PRIMARY KEY,
                name TEXT NOT NULL,
                coord_x DECIMAL,
                coord_y INTEGER NOT NULL CHECK (coord_y > -471),
                creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                price DECIMAL CHECK (price > 0),
                discount DECIMAL NOT NULL CHECK (discount > 0 AND discount <= 100),
                refundable BOOLEAN,
                type TICKET_TYPE,
                person_height BIGINT CHECK (person_height > 0),
                person_nationality COUNTRY,
                owner_login VARCHAR(255) REFERENCES users(login)
            );
            """;

    public static String addUser =
            """
            INSERT INTO users (login, password, salt) VALUES (?, ?, ?);
            """;

    public static String getUserByLogin =
            """
            SELECT * FROM users WHERE (login = ?);
            """;

    public static String addTicket =
            """
            INSERT INTO tickets
                (name, coord_x, coord_y, price, discount, refundable, type, person_height, person_nationality, owner_login, creation_date)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, DEFAULT)
            RETURNING id;
            """;

    public static String getAllTickets =
            """
            SELECT * FROM tickets;
            """;

    public static String getAllTicketsByUser =
            """
            SELECT * FROM tickets
            WHERE (owner_login = ?);
            """;

    public static String updateTicketById =
            """
            UPDATE
                tickets
            SET
                (name, coord_x, coord_y, price, discount, refundable, type, person_height, person_nationality) =
            (?, ?, ?, ?, ?, ?, ?, ?, ?)
            WHERE
                (id = ?) AND (owner_login = ?);
            """;

    public static String deleteTicketByIdFromUser =
            """
            DELETE FROM tickets
            WHERE
                (id = ?) AND (owner_login = ?);
            """;

    public static String deleteAllTicketsFromUser =
            """
            DELETE FROM tickets
            WHERE
                (owner_login = ?);
            """;
}
