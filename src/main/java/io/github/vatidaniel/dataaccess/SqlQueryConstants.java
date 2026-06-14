package io.github.vatidaniel.dataaccess;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author tinhnv
 * @since Oct 31, 2023
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SqlQueryConstants {

    protected static final String BACKTICK = "`";
    protected static final String SINGLE_QUOTE = "'";
    protected static final String DOUBLE_QUOTE = "\"";
    protected static final String EQUAL = "=";
    protected static final String SPACE = " ";
    protected static final String EMPTY = "";

    protected static final String AND_KEYWORD = "AND";
    protected static final String MAX_FORMAT = " MAX(%s) ";
    protected static final String MIN_FORMAT = " MIN(%s) ";

    protected static final String AND_FORMAT = "AND ( %s )";
    protected static final String IN_FORMAT = "IN ( %s ) ";
    protected static final String NOT_IN_FORMAT = "NOT IN ( %s )";
    protected static final String IF_FUNCTION_FORMAT = "IF (%s, %s, %s)";

    public static final String SELECT_KEYWORD = "SELECT ";
    public static final String FROM_KEYWORD = "FROM ";
    public static final String WHERE_KEYWORD = "WHERE ";
    public static final String LIMIT_KEYWORD = "LIMIT ";
    public static final String OFFSET_KEYWORD = "OFFSET ";
    public static final String INNER_JOIN_KEYWORD = "INNER JOIN ";
    public static final String LEFT_JOIN_KEYWORD = "LEFT JOIN ";
    public static final String GROUP_BY_KEYWORD = "GROUP BY ";
    public static final String ORDER_BY_KEYWORD = "ORDER BY ";

    public static final String COUNT_ALL = "COUNT(1)";
    public static final String COUNTER = "COUNTER";
    public static final String COUNT_FUNCTION_FORMAT = "COUNT(1) OVER(%s) AS " + COUNTER;
    public static final String CONCAT_FUNCTION_FORMAT = "CONCAT( %s )";

    public static final String NULL = " NULL ";
    public static final String IS_NULL = " IS NULL ";
    public static final String IS_NOT_NULL = " IS NOT NULL ";
    public static final String AS = " AS ";
    public static final String ON = " ON ";
    public static final String DOT = ".";
    public static final String COMMA = ",";
    public static final String OPEN_BRACKET = "(";
    public static final String CLOSE_BRACKET = ")";
    public static final String QUESTION_MARK = "?";
    public static final String SCREAMER = "!";

    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";

    /**
     * Wrap a value in single quotes for use as a SQL string literal, escaping any embedded single quote
     * by doubling it (the SQL-standard rule), e.g. {@code O'Brien} -&gt; {@code 'O''Brien'}.
     */
    public static String singleQuoteWrap(String s) {
        return SINGLE_QUOTE + s.replace(SINGLE_QUOTE, SINGLE_QUOTE + SINGLE_QUOTE) + SINGLE_QUOTE;
    }

    /**
     * Wrap an identifier in the given delimiters, escaping any occurrence of the closing delimiter by
     * doubling it (the SQL-standard escaping rule), e.g. {@code a`b} -&gt; {@code `a``b`}.
     */
    public static String quoteIdentifier(String identifier, String open, String close) {
        return open + identifier.replace(close, close + close) + close;
    }

    public static String backtickWrap(String s) {
        return quoteIdentifier(s, BACKTICK, BACKTICK);
    }

    public static String roundBracketWrap(String s) {
        return OPEN_BRACKET + s + CLOSE_BRACKET;
    }

}
