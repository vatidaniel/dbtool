package io.github.vatidaniel.common;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * @author tinhnv
 * @since Dec 19, 2023
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public interface EnumResponse {
    String getLabel();
    String getValue();
}
