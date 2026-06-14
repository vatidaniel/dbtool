package io.github.vatisteve.metadata.core;

import io.github.vatisteve.common.EnumResponse;

import java.util.Objects;

/**
 * @author tinhnv
 * @since Dec 19, 2023
 */
public interface DataType extends EnumResponse {

    String name();

    default String getKeyWord() {
        return name();
    }

    /**
     * Escape hatch for a type keyword that has no dialect enum constant (a vendor-specific or newly
     * introduced type). The returned {@code DataType} renders as the given keyword and reports an
     * unknown category ({@link #getParent()} is {@code null}), so category-driven logic
     * (e.g. default-value quoting) treats it the same way the old raw-{@code String} model did.
     *
     * @param keyWord the type keyword as it should appear in SQL (e.g. {@code "VECTOR"}); must not be blank
     */
    static DataType of(String keyWord) {
        return new RawDataType(keyWord);
    }

    default String getLabel() {
        return name();
    }

    default String getValue() {
        return name();
    }

    DataType getParent();

    boolean isEnable();

    enum BasicDataType implements DataType {

        NUMERIC, STRING, TEMPORAL, SPATIAL;

        @Override
        public DataType getParent() {
            return null;
        }

        @Override
        public boolean isEnable() {
            return false;
        }
    }

    /**
     * A {@link DataType} backed by a raw keyword, created via {@link DataType#of(String)} for types
     * that are not modelled by a dialect enum.
     */
    final class RawDataType implements DataType {

        private final String keyWord;

        RawDataType(String keyWord) {
            if (keyWord == null || keyWord.trim().isEmpty()) {
                throw new IllegalArgumentException("data type keyword must not be blank");
            }
            this.keyWord = keyWord;
        }

        @Override
        public String name() {
            return keyWord;
        }

        @Override
        public DataType getParent() {
            return null;
        }

        @Override
        public boolean isEnable() {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RawDataType)) return false;
            return keyWord.equals(((RawDataType) o).keyWord);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyWord);
        }

        @Override
        public String toString() {
            return keyWord;
        }
    }

}
