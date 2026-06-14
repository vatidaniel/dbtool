package io.github.vatidaniel.common;

/**
 * @author tinhnv
 * @since Dec 19, 2023
 */
public interface EnumResponse {
    String name();
    String getLabel();
    default String getValue() { return name(); }
}
