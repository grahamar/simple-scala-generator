package com.meetup.codegen;

import io.grhodes.simple.codegen.SimpleScalaCodegen;
import io.swagger.codegen.CodegenType;

public final class TestScalaCodegen {

    static private final class AdHoc extends SimpleScalaCodegen {
        private final String name;
        private final CodegenType tag;

        private AdHoc(String name, CodegenType tag) {
            this.name = name;
            this.tag = tag;
        }

        @Override
        public CodegenType getTag() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String toString() {
            return "AdHoc{" +
                    "name='" + name + '\'' +
                    ", tag=" + tag +
                    '}';
        }
    }

    public static SimpleScalaCodegen get(CodegenType tag) {
        return new AdHoc("test-simple-scala-" + tag.name(), tag);
    }

    public static SimpleScalaCodegen getServer() {
        return get(CodegenType.SERVER);
    }
}
