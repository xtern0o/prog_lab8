package org.example.client.utils;

public record RectCoords(float x1, float y1, float x2, float y2) {
    /**
     * Проверка принадлежности точки прямоугольнику
     * @param x координата x
     * @param y координата y
     * @return внутри или нет
     */
    public boolean isInside(double x, double y) {
        return x1 <= x && x <= x2 && y1 <= y && y <= y2;
    }
}
