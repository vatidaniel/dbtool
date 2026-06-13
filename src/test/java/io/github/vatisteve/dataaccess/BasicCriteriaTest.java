package io.github.vatisteve.dataaccess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tinhnv
 */
class BasicCriteriaTest {

    @Test
    void equalWithSingleQuote_wrapsValue() {
        String criteria = new BasicCriteria("name").equalWithSingleQuote("john").toCriteriaString();
        assertTrue(criteria.contains("name = 'john'"), criteria);
    }

    @Test
    void inFormat_quotesEachElement() {
        String criteria = new BasicCriteria("status").append(
            new BasicCriteria("").inFormat("A", "B").toCriteriaString()).toCriteriaString();
        assertTrue(criteria.contains("'A', 'B'"), criteria);
        assertTrue(criteria.contains("IN"), criteria);
    }

    @Test
    void notInFormat_quotesEachElement() {
        String criteria = new BasicCriteria("status").notInFormat("X", "Y").toCriteriaString();
        assertTrue(criteria.contains("NOT IN"), criteria);
        assertTrue(criteria.contains("'X', 'Y'"), criteria);
    }

    @Test
    void and_chainsAdditionalCondition() {
        String criteria = new BasicCriteria("a")
            .equalWithNumber(1)
            .and("b IS NULL")
            .toCriteriaString();
        assertTrue(criteria.contains("a = 1"), criteria);
        assertTrue(criteria.contains("AND b IS NULL"), criteria);
    }
}
