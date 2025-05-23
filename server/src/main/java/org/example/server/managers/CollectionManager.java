package org.example.server.managers;

import lombok.Getter;
import org.example.common.entity.Ticket;
import org.example.common.exceptions.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Класс для управления коллекцией
 * @author maxkarn
 */
public class CollectionManager {
    public final static Logger logger = LoggerFactory.getLogger(CollectionManager.class);
    /**
     * Коллекция билетов текущего сеанса
     * final, чтобы во время рантайма не рпоизошло случайной замены
     */
    @Getter
    private static final PriorityBlockingQueue<Ticket> collection = new PriorityBlockingQueue<>();

    /**
     * Время инициализации коллекции
     * Время инициализации объекта CollectionManager
     */
    @Getter
    private static final Date initDate = new Date();

    /**
     * Метод присваивает коллекции передаваемое значение, если элементы коллекции корректны;
     * synchronized, так как между двумя атомарными операциями другой поток может изменить данные
     * @param collection новая коллекция
     * @return true если успешно, false если не прошла валидация одного из элементов
     */
    public static synchronized boolean setCollection(PriorityBlockingQueue<Ticket> collection) {
        if (collection == null) {
            return true;
        }
        if (!CollectionManager.allIdsAreUnique(collection) || !collection.stream().allMatch(Ticket::validate)) {
            return false;
        }

        CollectionManager.collection.clear();
        CollectionManager.collection.addAll(collection);

        logger.info("Коллекция обновлена");
        return true;
    }

    /**
     * Получение типа коллекции
     * @return класс объекта коллекции
     */
    public static String getTypeOfCollection() {
        return collection.getClass().getName();
    }

    /**
     * Возвращает размер коллекции
     * @return число элементов в коллекции
     */
    public static int getCollectionSize() {
        return collection.size();
    }

    /**
     * Находит объект в коллекции по его id
     * @param id айди.
     * @return Объект из коллекции или null, если его не существует
     */
    public static Ticket getElementById(Integer id) {
        return collection.stream()
                .filter(ticket -> Objects.equals(ticket.getId(), id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Очищает коллекцию
     */
    public static void clearCollection() {
        collection.clear();
    }

    /**
     * Удаляет элемент из коллекции по его id
     * @param id id элемента
     * @return true если элемент с таким id есть и удален, и false если элемент не найден
     */
    public static boolean removeById(int id) {
        boolean deleted = collection.removeIf(ticket -> ticket.getId() == id);
        if (deleted) {
            logger.info("Элемент с id={} был успешно удален", id);
        }
        else {
            logger.warn("Элемент с id={} не найден", id);
        }
        return deleted;
    }

    /**
     * Проверка на уникальность всех id в коллекции билетов
     * @param collection коллекция
     * @return уникалны ли айдишники
     */
    public static boolean allIdsAreUnique(Collection<Ticket> collection) {
        return collection.stream()
                .map(Ticket::getId)
                .distinct()
                .count() == collection.size();
    }

    /**
     * Добавляет элемент в коллекцию предварительно проведя контрольную валидацию
     * @param ticket новый элемент
     * @throws ValidationError в случае неудачного прохождения валидации
     */
    public static void addElement(Ticket ticket) throws ValidationError {
        if (ticket.validate()) {
            collection.add(ticket);
            logger.info("Добавлен новый элемент с id={}", ticket.getId());
            return;
        }
        throw new ValidationError(ticket);
    }
}
