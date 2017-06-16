package com.meetup.codegen

import io.grhodes.simple.codegen.SimpleScalaCodegen

import io.swagger.codegen.CodegenProperty
import org.scalatest.{FunSpec, Matchers}

class ModelEnumNamingTest extends FunSpec with Matchers {

  val codeGen = TestScalaCodegen.getServer

  describe("toEnumName") {
    it("should capitalize the name") {
      val property = new CodegenProperty
      property.name = "psp"
      codeGen.toEnumName(property) shouldBe "Psp"
    }

    it("should camelize the name and supplant any snake_casing") {
      val property = new CodegenProperty
      property.name = "enum_property"
      codeGen.toEnumName(property) shouldBe "EnumProperty"
    }
  }

  describe("toEnumVarName") {
    it("should camelize and remove spaces from the value") {
      val name = codeGen.toEnumVarName("hello there", "some_data_type")
      name shouldBe "HelloThere"
    }

    it("should camelize and remove underscores from the value") {
      val name = codeGen.toEnumVarName("hello_there", "some_data_type")
      name shouldBe "HelloThere"
    }

    it("should prefix numeric values with the word \"Number\"") {
      SimpleScalaCodegen.NUMBER_TYPES.foreach { t =>
        val name = codeGen.toEnumVarName("1", t)
        name shouldBe "Number1"
      }
    }
  }

  describe("toEnumValue") {
    it("should double quote non-numeric values") {
      val value = codeGen.toEnumValue("non numeric", "some_data_type")
      value shouldBe "\"non numeric\""
    }

    it("should do nothing for numeric values") {
      SimpleScalaCodegen.NUMBER_TYPES.foreach { t =>
        codeGen.toEnumValue("1", t) shouldBe "1"
      }
    }
  }
}
