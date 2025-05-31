package org.example.client.gui.filters;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import org.example.common.entity.Country;
import org.example.common.entity.Ticket;
import org.example.common.entity.TicketType;

import java.time.ZonedDateTime;
import java.util.function.Predicate;

/**
 * Класс для управления фильтрами таблицы билетов
 */
@Getter
public class TableFilterManager {
    private static final StringProperty nameStartsWith = new SimpleStringProperty();
    private static final StringProperty ownerLoginEqualsProp = new SimpleStringProperty();

    @Getter
    private static Double priceMin;
    @Getter
    private static Double priceMax;
    @Getter
    private static Float discountMin;
    @Getter
    private static Float discountMax;
    @Getter
    private static Float coordXMin;
    @Getter
    private static Float coordXMax;
    @Getter
    private static Double coordYMin;
    @Getter
    private static Double coordYMax;
    @Getter
    private static Long heightMin;
    @Getter
    private static Long heightMax;
    @Getter
    private static Integer idEquals;

    @Getter
    private static ZonedDateTime dateFrom;
    @Getter
    private static ZonedDateTime dateTo;

    @Getter
    private static TicketType ticketType;
    @Getter
    private static Country nationality;

    // проперти для активных фильтров для отслеживания изменений
    @Getter private static final BooleanProperty nameFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty priceFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty discountFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty coordFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty heightFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty dateFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty typeFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty nationalityFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty ownerLoginFilterActive = new SimpleBooleanProperty(false);
    @Getter private static final BooleanProperty idFilterActive = new SimpleBooleanProperty(false);

    /**
     * Создает предикат на основе текущих настроек фильтров
     */
    public static Predicate<Ticket> createPredicate() {
        return ticket -> {
            if (nameFilterActive.get() && nameStartsWith.get() != null && !nameStartsWith.get().isEmpty()) {
                if (!ticket.getName().toLowerCase().startsWith(nameStartsWith.get().toLowerCase())) {
                    return false;
                }
            }

            if (priceFilterActive.get()) {
                if (priceMin != null && ticket.getPrice() < priceMin) return false;
                if (priceMax != null && ticket.getPrice() > priceMax) return false;
            }

            if (discountFilterActive.get()) {
                if (discountMin != null && ticket.getDiscount() < discountMin) return false;
                if (discountMax != null && ticket.getDiscount() > discountMax) return false;
            }

            if (coordFilterActive.get()) {
                if (coordXMin != null && ticket.getCoordinates().getX() < coordXMin) return false;
                if (coordXMax != null && ticket.getCoordinates().getX() > coordXMax) return false;
                if (coordYMin != null && ticket.getCoordinates().getY() < coordYMin) return false;
                if (coordYMax != null && ticket.getCoordinates().getY() > coordYMax) return false;
            }

            if (heightFilterActive.get()) {
                if (heightMin != null && ticket.getPerson().getHeight() < heightMin) return false;
                if (heightMax != null && ticket.getPerson().getHeight() > heightMax) return false;
            }

            if (dateFilterActive.get()) {
                if (dateFrom != null && ticket.getCreationDate().isBefore(dateFrom)) return false;
                if (dateTo != null && ticket.getCreationDate().isAfter(dateTo)) return false;
            }

            if (typeFilterActive.get() && ticketType != null) {
                if (ticket.getType() != ticketType) return false;
            }

            if (nationalityFilterActive.get() && nationality != null) {
                if (ticket.getPerson().getNationality() != nationality) return false;
            }

            if (ownerLoginFilterActive.get() && ownerLoginEqualsProp.get() != null && !ownerLoginEqualsProp.get().isEmpty()) {
                if (!ticket.getOwnerLogin().equals(ownerLoginEqualsProp.get())) return false;
            }

            if (idFilterActive.get() && idEquals != null) {
                if (!ticket.getId().equals(idEquals)) return false;
            }

            return true;
        };
    }

    public static String getNameStartsWith() {
        return nameStartsWith.get();
    }

    public static StringProperty nameStartsWithProperty() {
        return nameStartsWith;
    }

    public static StringProperty ownerLoginProperty() {
        return ownerLoginEqualsProp;
    }

    public static void setNameStartsWith(String nameStartsWith) {
        TableFilterManager.nameStartsWith.set(nameStartsWith);
    }

    public static BooleanProperty nameFilterActiveProperty() {
        return nameFilterActive;
    }

    public static void setNameFilterActive(boolean active) {
        TableFilterManager.nameFilterActive.set(active);
    }

    public static void setOwnerLoginFilterActive(boolean active) {
        TableFilterManager.ownerLoginFilterActive.set(active);
    }

    public static void setPriceRange(Double min, Double max) {
        TableFilterManager.priceMin = min;
        TableFilterManager.priceMax = max;
        TableFilterManager.priceFilterActive.set(min != null || max != null);
    }

    public static void setIdEquals(Integer id) {
        TableFilterManager.idEquals = id;
        TableFilterManager.idFilterActive.set(id != null);
    }

    public static void setDiscountRange(Float min, Float max) {
        TableFilterManager.discountMin = min;
        TableFilterManager.discountMax = max;
        TableFilterManager.discountFilterActive.set(min != null || max != null);
    }

    public static void setCoordXRange(Float min, Float max) {
        TableFilterManager.coordXMin = min;
        TableFilterManager.coordXMax = max;
        TableFilterManager.coordFilterActive.set(coordXMin != null || coordXMax != null || coordYMin != null || coordYMax != null);
    }

    public static void setCoordYRange(Double min, Double max) {
        TableFilterManager.coordYMin = min;
        TableFilterManager.coordYMax = max;
        TableFilterManager.coordFilterActive.set(coordXMin != null || coordXMax != null || coordYMin != null || coordYMax != null);
    }

    public static void setHeightRange(Long min, Long max) {
        TableFilterManager.heightMin = min;
        TableFilterManager.heightMax = max;
        TableFilterManager.heightFilterActive.set(min != null || max != null);
    }

    public static void setDateRange(ZonedDateTime from, ZonedDateTime to) {
        TableFilterManager.dateFrom = from;
        TableFilterManager.dateTo = to;
        TableFilterManager.dateFilterActive.set(from != null || to != null);
    }

    public static void setTicketType(TicketType type) {
        TableFilterManager.ticketType = type;
        TableFilterManager.typeFilterActive.set(type != null);
    }

    public static void setNationality(Country nationality) {
        TableFilterManager.nationality = nationality;
        TableFilterManager.nationalityFilterActive.set(nationality != null);
    }

    public static void setOwnerLogin(String login) {
        TableFilterManager.ownerLoginEqualsProp.set(login);
        TableFilterManager.ownerLoginFilterActive.set(login != null);
    }

    /**
     * Сброс всех существующих фильтров
     */
    public static void resetAllFilters() {
        nameStartsWith.set("");
        nameFilterActive.set(false);

        priceMin = null;
        priceMax = null;
        priceFilterActive.set(false);

        idEquals = null;
        idFilterActive.set(false);

        discountMin = null;
        discountMax = null;
        discountFilterActive.set(false);

        coordXMin = null;
        coordXMax = null;
        coordYMin = null;
        coordYMax = null;
        coordFilterActive.set(false);

        heightMin = null;
        heightMax = null;
        heightFilterActive.set(false);

        dateFrom = null;
        dateTo = null;
        dateFilterActive.set(false);

        ticketType = null;
        typeFilterActive.set(false);

        nationality = null;
        nationalityFilterActive.set(false);

        ownerLoginEqualsProp.set("");
        ownerLoginFilterActive.set(false);

    }
}