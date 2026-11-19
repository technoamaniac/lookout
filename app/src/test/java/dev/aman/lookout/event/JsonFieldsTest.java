package dev.aman.lookout.event;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
    public void writesBooleans() {
        assertEquals("{\"a\":true}", new JsonFields().bool("a", true).compact());
    }

    @Test
    public void copiesAreIndependent() {
        JsonFields original = new JsonFields().string("a", "x");
        original.copy().number("b", 1);
        assertEquals("{\"a\":\"x\"}", original.compact());
    }

    @Test
    public void prettyPrintsOneFieldPerLine() {
        assertEquals("{\n  \"a\": \"x\",\n  \"b\": 2\n}", new JsonFields().string("a", "x").number("b", 2).pretty());
    }

    @Test
    public void writesArraysOfObjects() {
        assertEquals("{\"a\":[{\"b\":1},{\"c\":\"x\"}],\"d\":[]}", new JsonFields()
                .objects("a", List.of(new JsonFields().number("b", 1), new JsonFields().string("c", "x")))
                .objects("d", List.of()).compact());
    }

    @Test
    public void prettyPrintsNestedObjectsAndArraysIndented() {
        JsonFields fields = new JsonFields().string("a", "x")
                .objects("b", List.of(
                        new JsonFields().string("c", "y").number("d", 1), new JsonFields().number("e", 2)))
                .object("f", new JsonFields().bool("g", true));
        assertEquals("{\n  \"a\": \"x\",\n  \"b\": [\n    {\n      \"c\": \"y\",\n      \"d\": 1\n    },\n"
                + "    {\n      \"e\": 2\n    }\n  ],\n  \"f\": {\n    \"g\": true\n  }\n}", fields.pretty());
    }

    @Test
    public void prettyKeepsEmptyArraysAndObjectsOnOneLine() {
        assertEquals("{\n  \"a\": [],\n  \"b\": {}\n}",
                new JsonFields().objects("a", List.of()).object("b", new JsonFields()).pretty());
        assertEquals("{}", new JsonFields().pretty());
    }

    @Test
    public void prettyLeavesBracketsCommasColonsAndEscapedQuotesInsideStringsAlone() {
        String tricky = "{[a,b]: \"c\"}";
        assertEquals("{\n  \"a\": \"{[a,b]: \\\"c\\\"}\"\n}", new JsonFields().string("a", tricky).pretty());
    }

    @Test
    public void nestedObjectsAndArraysAreCopiedWhenAdded() {
        JsonFields inner = new JsonFields().number("a", 1);
        List<JsonFields> list = new ArrayList<>(List.of(inner));
        JsonFields outer = new JsonFields().object("o", inner).objects("l", list);
        inner.number("b", 2);
        list.clear();
        assertEquals("{\"o\":{\"a\":1},\"l\":[{\"a\":1}]}", outer.compact());
    }
}
