package com.meetup.codegen

import io.swagger.models.properties._
import org.scalatest.{FunSpec, Matchers}

class MappedSwaggerDatePropertiesTest extends FunSpec with Matchers {

  val codeGen = TestScalaCodegen.getServer

  val swaggerMappedTypes =
    Map[String, (String, String => AbstractProperty)](
      ("date", ("LocalDate", mkProp(new DateProperty))),
      ("date-time", ("ZonedDateTime", mkProp(new DateTimeProperty)))
    )

  describe("a swagger date/time property") {
    it("should map to the expected type") {
      swaggerMappedTypes.foreach {
        case (from, (to, f)) =>
          codeGen.getSwaggerType(f(from)) shouldBe to
      }
    }
  }

}
