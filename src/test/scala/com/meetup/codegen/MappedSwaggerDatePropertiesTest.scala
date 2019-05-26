package com.meetup.codegen

import io.swagger.v3.oas.models.media._
import org.scalatest.{FunSpec, Matchers}

class MappedSwaggerDatePropertiesTest extends FunSpec with Matchers {

  val codeGen = TestScalaCodegen.getServer

  val swaggerMappedTypes =
    Map[String, (String, String => Schema[_])](
      ("date", ("LocalDate", mkSchema(new DateSchema))),
      ("date-time", ("ZonedDateTime", mkSchema(new DateTimeSchema)))
    )

  describe("a swagger date/time property") {
    it("should map to the expected type") {
      swaggerMappedTypes.foreach {
        case (from, (to, f)) =>
          codeGen.getSchemaType(f(from)) shouldBe to
      }
    }
  }

}
