package io.github.vatidaniel.dataaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author tinhnv
 * @since Oct 31, 2023
 *
 * @apiNote used for mariadb, mysql query format
 *
 * <p>Value methods come in two flavors: the bound ones ({@link #equal}, {@link #in}, ...) emit
 * {@code ?} placeholders and record the value in {@link #parameters()} for a {@link PreparedStatement},
 * while the older {@code *WithSingleQuote}/{@code *Format} methods inline the value into the SQL text and
 * are deprecated because they are open to SQL injection.
 */
public class BasicCriteria extends SqlQueryConstants {

    private final StringBuilder criteria;
    private final List<Object> parameters = new ArrayList<>();

    public BasicCriteria(String criteria) {
        this.criteria = new StringBuilder(criteria);
    }

    /** Values bound by the {@code ?} placeholders this criteria emitted, in order. */
    public List<Object> parameters() {
        return parameters;
    }

    // --- bound value methods (preferred) ---

    public BasicCriteria equal(Object value) {
        criteria.append(SPACE).append(EQUAL).append(SPACE).append(QUESTION_MARK).append(SPACE);
        parameters.add(value);
        return this;
    }

    public BasicCriteria notEqual(Object value) {
        criteria.append(SPACE).append(SCREAMER).append(EQUAL).append(SPACE).append(QUESTION_MARK).append(SPACE);
        parameters.add(value);
        return this;
    }

    public BasicCriteria in(Object... values) {
        return appendInClause(IN_FORMAT, values);
    }

    public BasicCriteria in(Collection<?> values) {
        return appendInClause(IN_FORMAT, values == null ? null : values.toArray());
    }

    public BasicCriteria notIn(Object... values) {
        return appendInClause(NOT_IN_FORMAT, values);
    }

    public BasicCriteria notIn(Collection<?> values) {
        return appendInClause(NOT_IN_FORMAT, values == null ? null : values.toArray());
    }

    private BasicCriteria appendInClause(String format, Object[] values) {
        requireNonEmpty(values);
        String placeholders = Arrays.stream(values).map(v -> QUESTION_MARK).collect(Collectors.joining(COMMA + SPACE));
        criteria.append(SPACE).append(String.format(format, placeholders)).append(SPACE);
        parameters.addAll(Arrays.asList(values));
        return this;
    }

    private static void requireNonEmpty(Object[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("IN / NOT IN requires at least one value");
        }
    }

    /** Append another criteria with {@code AND}, merging its bound parameters in order. */
    public BasicCriteria and(BasicCriteria other) {
        criteria.append(SPACE).append(AND_KEYWORD).append(SPACE).append(other.toCriteriaString()).append(SPACE);
        parameters.addAll(other.parameters());
        return this;
    }

    // --- inlining value methods (deprecated: prone to SQL injection) ---

    /** @deprecated inlines the value into the SQL text; use {@link #equal(Object)} instead. */
    @Deprecated(since = "0.1.0", forRemoval = true)
    public BasicCriteria equalWithSingleQuote(String query) {
        criteria.append(SPACE).append(EQUAL).append(SPACE).append(SINGLE_QUOTE).append(query).append(SINGLE_QUOTE).append(SPACE);
        return this;
    }

    /** @deprecated inlines the value into the SQL text; use {@link #notEqual(Object)} instead. */
    @Deprecated(since = "0.1.0", forRemoval = true)
    public BasicCriteria notEqualWithSingleQuote(String query) {
        criteria.append(SPACE).append(SCREAMER).append(EQUAL).append(SPACE).append(SINGLE_QUOTE).append(query).append(SINGLE_QUOTE).append(SPACE);
        return this;
    }

    public BasicCriteria equalWithNumber(Number number) {
        criteria.append(SPACE).append(EQUAL).append(SPACE).append(number).append(SPACE);
        return this;
    }

    public BasicCriteria notEqualWithNumber(Number number) {
        criteria.append(SPACE).append(SCREAMER).append(EQUAL).append(SPACE).append(number).append(SPACE);
        return this;
    }

    public BasicCriteria and(String query) {
        criteria.append(SPACE).append(AND_KEYWORD).append(SPACE).append(query).append(SPACE);
        return this;
    }

    public BasicCriteria andFormat(String query) {
        criteria.append(SPACE).append(String.format(AND_FORMAT, query)).append(SPACE);
        return this;
    }

    /** @deprecated inlines the values into the SQL text; use {@link #in(Object...)} instead. */
    @Deprecated(since = "0.1.0", forRemoval = true)
    public BasicCriteria inFormat(String... elements) {
        requireNonEmpty(elements);
        String wrappedElement = Arrays.stream(elements).map(SqlQueryConstants::singleQuoteWrap).collect(Collectors.joining(COMMA + SPACE));
        criteria.append(SPACE).append(String.format(IN_FORMAT, wrappedElement)).append(SPACE);
        return this;
    }

    /** @deprecated inlines the values into the SQL text; use {@link #notIn(Object...)} instead. */
    @Deprecated(since = "0.1.0", forRemoval = true)
    public BasicCriteria notInFormat(String... elements) {
        requireNonEmpty(elements);
        String wrappedElement = Arrays.stream(elements).map(SqlQueryConstants::singleQuoteWrap).collect(Collectors.joining(COMMA + SPACE));
        criteria.append(SPACE).append(String.format(NOT_IN_FORMAT, wrappedElement)).append(SPACE);
        return this;
    }

    public BasicCriteria isTrue() {
        criteria.append(SPACE).append(EQUAL).append(1);
        return this;
    }

    public BasicCriteria append(String s) {
        criteria.append(SPACE).append(s).append(SPACE);
        return this;
    }

    public String toCriteriaString() {
        return criteria.toString();
    }
}
