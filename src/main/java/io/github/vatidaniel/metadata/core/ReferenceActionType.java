package io.github.vatidaniel.metadata.core;

import io.github.vatidaniel.common.EnumResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author tinhnv
 * @since Dec 19, 2023
 */
@Getter
@RequiredArgsConstructor
public enum ReferenceActionType implements EnumResponse {

    NO_ACTION("No action"),
    CASCADE("Cascade"),
    SET_NULL("Set NULL"),
    SET_DEFAULT("Set default");

    private final String label;

}
