package org.example.server.managers;

import org.apache.commons.codec.binary.Hex;
import org.example.common.dtp.User;
import org.example.common.entity.*;
import org.example.server.Main;
import org.example.server.utils.DatabaseInstructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.time.ZoneId;
import java.util.concurrent.PriorityBlockingQueue;

public class DatabaseManager {
    public static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final static String DB_URL = Main.dotenv.get("DB_URL");
    private final static String DB_URL_HELIOS = Main.dotenv.get("DB_URL_HELIOS");
    private final static String DB_USER = Main.dotenv.get("DB_USER");
    private final static String DB_PASSWORD = Main.dotenv.get("DB_PASSWORD");
    private final static String DB_PEPPER = Main.dotenv.get("DB_PEPPER");

    private Connection connection;
    private MessageDigest md;

    public DatabaseManager() {
        try {
            this.md = MessageDigest.getInstance("MD5");

            this.connect();
            this.initDataBase();
        } catch (SQLException sqlException) {
            logger.warn("БД уже создана или возникла иная ошибка: {}", sqlException.getMessage());
        } catch (NoSuchAlgorithmException algorithmException) {
            logger.error("Такого алгоритма для хэширования нет");
            logger.debug(algorithmException.getMessage());
        }
    }

    /**
     * Метод для подключения к БД
     */
    public void connect() {
        try {
            this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        } catch (SQLException e) {
            try {
                this.connection = DriverManager.getConnection(DB_URL_HELIOS, DB_USER, DB_PASSWORD);
            } catch (SQLException e1) {
                logger.error("Невозможно подключиться к базе данных");
                logger.error(e.getMessage());
                logger.error(e1.getMessage());
                System.exit(1);
            }
        }
    }

    public void commit() throws SQLException {
        this.connection.commit();
    }

    /**
     * Инициализация базы данных если она не создана
     * @throws SQLException если она уже существует, или возникла иная ошибка выполнения
     */
    private void initDataBase() throws SQLException {
        connection.prepareStatement(DatabaseInstructions.createDatabase).execute();
        logger.info("База данных успешно создана");
    }

    /**
     * Добавление нового пользователя в базу данных
     * @param user объект юзера из commons.dtp
     * @throws SQLException если юзер с таким логином уже существует или просто ошибка валидации/создания
     */
    public void addUser(User user) throws SQLException {
        String login = user.login();
        String salt = generateRandomString16();
        String hashedPassword = hashPassword(user.password(), salt);

        PreparedStatement ps = connection.prepareStatement(DatabaseInstructions.addUser);

        ps.setString(1, login);
        ps.setString(2, hashedPassword);
        ps.setString(3, salt);
        ps.execute();

        logger.info("Добавлен новый пользователь: {}", user);
    }

    public User getUserByLogin(String login) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(DatabaseInstructions.getUserByLogin)) {
            ps.setString(1, login);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getString("login"),
                            resultSet.getString("password")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Проверка соответствия введенного пароля пользователя и фактического
     * @param inputUser объект юзера
     * @return OK | NE OK
     */
    public boolean checkUserData(User inputUser) {
        try {
            String login = inputUser.login();
            PreparedStatement ps = connection.prepareStatement(DatabaseInstructions.getUserByLogin);
            ps.setString(1, login);

            ResultSet resultSet = ps.executeQuery();

            if (resultSet == null) return false;

            if (resultSet.next()) {
                String salt = resultSet.getString("salt");
                String inputUserPasswordHash = hashPassword(inputUser.password(), salt);
                return inputUserPasswordHash.equals(resultSet.getString("password"));
            }
            return false;
        } catch (SQLException e) {
            logger.error("Wrong SQL instruction");
            logger.debug(e.getMessage());
            return false;
        }
    }

    /**
     * Добавление нового билета в БД
     * @param ticket Объект билета
     * @return id нового билета, -1 при ошибке
     */
    public int addTicket(Ticket ticket) {
        try {
            PreparedStatement ps = connection.prepareStatement(DatabaseInstructions.addTicket);

            ps.setString(1, ticket.getName());
            ps.setFloat(2, ticket.getCoordinates().getX());
            ps.setInt(3, ticket.getCoordinates().getY());
            ps.setDouble(4, ticket.getPrice());
            ps.setFloat(5, ticket.getDiscount());
            ps.setBoolean(6, ticket.isRefundable());
            ps.setObject(7, ticket.getType(), Types.OTHER);
            ps.setLong(8, ticket.getPerson().getHeight());
            ps.setObject(9, ticket.getPerson().getNationality(), Types.OTHER);
            ps.setString(10, ticket.getOwnerLogin());

            ResultSet resultSet = ps.executeQuery();

            if (!resultSet.next()) {
                logger.warn("Объект не добавлен в БД");
                return -1;
            }

            logger.info("Объект успешно добавлен в таблицу");
            return resultSet.getInt(1);

        } catch (SQLException sqlException) {
            logger.warn("Объект не добавлен в БД");
            logger.debug(sqlException.getMessage());
            return -1;
        }
    }

    /**
     * Обновление объекта в БД
     * @param ticket объект билета
     * @return 1, если успешно; 0 если не найден; -1 если ошибка выпоолнения
     */
    public int updateTicket(Ticket ticket) {
        try {
            PreparedStatement ps = connection.prepareStatement(DatabaseInstructions.updateTicketById);
            ps.setString(1, ticket.getName());
            ps.setFloat(2, ticket.getCoordinates().getX());
            ps.setInt(3, ticket.getCoordinates().getY());
            ps.setDouble(4, ticket.getPrice());
            ps.setFloat(5, ticket.getDiscount());
            ps.setBoolean(6, ticket.isRefundable());
            ps.setObject(7, ticket.getType(), Types.OTHER);
            ps.setLong(8, ticket.getPerson().getHeight());
            ps.setObject(9, ticket.getPerson().getNationality(), Types.OTHER);

            ps.setInt(10, ticket.getId());
            ps.setString(11, ticket.getOwnerLogin());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                logger.info("Изменение неудачно: билет с id={} от {} не найден", ticket.getId(), ticket.getOwnerLogin());
                return 0;
            }
            logger.info("Объект с id={} был успешно изменен", ticket.getId());
            return 1;
        } catch (SQLException sqlException) {
            logger.warn("Ошибка выполнения обновление объекта");
            logger.debug(sqlException.getMessage());
            return -1;
        }
    }

    public int deleteObjectsByUser(User user) {
        try {
            PreparedStatement ps = connection.prepareStatement(DatabaseInstructions.deleteAllTicketsFromUser);
            ps.setString(1, user.login());
            int affectedRows = ps.executeUpdate();
            logger.info("Удалено {} объектов от пользотвателя \"{}\"", affectedRows, user.login());
            return affectedRows;

        } catch (SQLException sqlException) {
            logger.warn("Не удалось удалить объекты");
            return -1;
        }
    }

    /**
     * Удаляет объект пользователя по id
     * @param user пользователь
     * @param id id объекта
     * @return 1, если успешно; 0 если не найден; -1 если ошибка
     */
    public int deleteObjectByIdFromUser(User user, int id) {
        try {
            PreparedStatement ps = connection.prepareStatement(DatabaseInstructions.deleteTicketByIdFromUser);
            ps.setInt(1, id);
            ps.setString(2, user.login());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                logger.info("Объект с id={} от пользователя {} не найден", id, user);
                return 0;
            }
            logger.info("Объект с id={} удален его создателем", id);
            return 1;
        } catch (SQLException sqlException) {
            logger.warn("Не удалось удалить объект с id={}. Ошибка выполнения запроса", id);
            return -1;
        }
    }

    public PriorityBlockingQueue<Ticket> loadCollection() {
        try {
            PriorityBlockingQueue<Ticket> collection = new PriorityBlockingQueue<>();

            PreparedStatement ps = connection.prepareStatement(DatabaseInstructions.getAllTickets);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                Coordinates coordinates = new Coordinates(
                        resultSet.getFloat("coord_x"),
                        resultSet.getInt("coord_y")
                );
                String personNationality = resultSet.getString("person_nationality");
                Person person = new Person(
                        resultSet.getLong("person_height"),
                        personNationality == null ? null : Country.valueOf(personNationality)
                );
                String ticketType = resultSet.getString("type");
                Ticket ticket = new Ticket(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        coordinates,
                        resultSet.getTimestamp("creation_date").toInstant().atZone(ZoneId.of("Europe/Moscow")),
                        resultSet.getDouble("price"),
                        resultSet.getFloat("discount"),
                        resultSet.getBoolean("refundable"),
                        ticketType == null ? null : TicketType.valueOf(ticketType),
                        person,
                        resultSet.getString("owner_login")
                );

                collection.add(ticket);
            }
            logger.info("Коллекция успешно инициализирована");
            return collection;

        } catch (SQLException sqlException) {
            logger.warn("Ошибка при загрузке коллекции: {}", sqlException.getMessage());
            return null;
        }
    }

    /**
     * get password hash
     * @param password password...
     * @param salt salt...
     * @return hashed password via algo
     */
    public String hashPassword(String password, String salt) {
        return hashMD5(DB_PEPPER + password + salt);
    }

    /**
     * MD5 hashing algorithm
     * @param input строка для хэширования
     * @return хэш исходной строки
     */
    private String hashMD5(String input) {
        byte[] messageDigest = md.digest(input.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * Метод для генерации случайной строки из 16 символов (aka для генерации соли)
     * @return random string
     */
    public static String generateRandomString16() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Hex.encodeHexString(salt);
    }

}
