package io.github.vatidaniel.dataaccess;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void equal_emitsPlaceholderAndRecordsParameter() {
        BasicCriteria c = new BasicCriteria("name").equal("john");
        assertTrue(c.toCriteriaString().contains("name = ?"), c.toCriteriaString());
        assertEquals(Arrays.asList("john"), c.parameters());
    }

    @Test
    void in_emitsPlaceholderPerValueAndRecordsParameters() {
        BasicCriteria c = new BasicCriteria("status").in("A", "B", "C");
        assertTrue(c.toCriteriaString().contains("IN ( ?, ?, ? )"), c.toCriteriaString());
        assertEquals(Arrays.asList("A", "B", "C"), c.parameters());
    }

    @Test
    void notIn_emitsPlaceholdersAndRecordsParameters() {
        BasicCriteria c = new BasicCriteria("status").notIn(Arrays.asList("X", "Y"));
        assertTrue(c.toCriteriaString().contains("NOT IN ( ?, ? )"), c.toCriteriaString());
        assertEquals(Arrays.asList("X", "Y"), c.parameters());
    }

    @Test
    void in_withNoValues_throwsInsteadOfEmittingEmptyInList() {
        assertThrows(IllegalArgumentException.class, () -> new BasicCriteria("status").in());
        assertThrows(IllegalArgumentException.class, () -> new BasicCriteria("status").notIn());
    }

    @Test
    void in_withNullValues_throwsIllegalArgumentNotNpe() {
        // a null argument should fail the same way an empty one does, not surface a raw NPE
        assertThrows(IllegalArgumentException.class, () -> new BasicCriteria("status").in((Object[]) null));
        assertThrows(IllegalArgumentException.class, () -> new BasicCriteria("status").notIn((Object[]) null));
        assertThrows(IllegalArgumentException.class, () -> new BasicCriteria("status").in((java.util.Collection<?>) null));
        assertThrows(IllegalArgumentException.class, () -> new BasicCriteria("status").notIn((java.util.Collection<?>) null));
        assertThrows(IllegalArgumentException.class, () -> new BasicCriteria("status").inFormat((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> new BasicCriteria("status").notInFormat((String[]) null));
    }

    @Test
    void and_withCriteria_mergesParametersInOrder() {
        BasicCriteria c = new BasicCriteria("a").equal(1)
            .and(new BasicCriteria("b").in("x", "y"));
        assertTrue(c.toCriteriaString().contains("a = ?"), c.toCriteriaString());
        assertTrue(c.toCriteriaString().contains("AND b"), c.toCriteriaString());
        assertEquals(Arrays.asList(1, "x", "y"), c.parameters());
    }
}
