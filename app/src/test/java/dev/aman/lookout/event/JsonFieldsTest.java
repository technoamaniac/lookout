package dev.aman.lookout.event;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JsonFieldsTest {
    @Test
    public void keepsFieldsInTheOrderAdded() {
        assertEquals("{\"b\":\"x\",\"a\":1}", new JsonFields().string("b", "x").number("a", 1).compact());
    }

    @Test
    public void leavesOutNullAndBlankOptionalFields() {
        assertEquals("{\"a\":\"x\"}",
                new JsonFields().string("a", "x").optionalString("b", null).optionalString("c", "  ").compact());
    }

    @Test
    public void escapesQuotesBackslashesAndControlChars() {
        assertEquals("{\"a\":\"q\\\"b\\\\c\\u000ad\"}", new JsonFields().string("a", "q\"b\\c\nd").compact());
    }

    @Test
    public void prettyPrintsOneFieldPerLine() {
        assertEquals("{\n  \"a\": \"x\",\n  \"b\": 2\n}", new JsonFields().string("a", "x").number("b", 2).pretty());
    }
}
