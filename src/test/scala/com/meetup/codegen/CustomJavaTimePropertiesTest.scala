package com.meetup.codegen

import io.swagger.v3.oas.models.media._
import org.scalatest.{FunSpec, Matchers}

class CustomJavaTimePropertiesTest extends FunSpec with Matchers {

  val codeGen = TestScalaCodegen.getServer

  val customMappedTypes =
    Map(
      "timestamp" -> "java.time.Instant",
      "local-time" -> "java.time.LocalTime",
      "local-date-time" -> "java.time.LocalDateTime",
      "date" -> "java.time.LocalDate",
      "date-time" -> "java.time.ZonedDateTime"
    )

  describe("a custom date/time property") {
    it("should map to the expected type") {
      customMappedTypes.foreach {
        case (from, to) =>
          val prop = mkSchema(new StringSchema)(from)
          codeGen.getSchemaType(prop) shouldBe to
      }
    }

    it("should not be mapped from any property type other than string") {
      val props: List[String => Schema[_]] =
        mkSchema(new BinarySchema) ::
          mkSchema(new BooleanSchema) ::
          mkSchema(new ByteArraySchema) ::
          mkSchema(new EmailSchema) ::
          mkSchema(new FileSchema) ::
          mkSchema(new IntegerSchema) ::
          mkSchema(new NumberSchema) ::
          mkSchema(new MapSchema) ::
          mkSchema(new ObjectSchema) ::
          mkSchema(new PasswordSchema) ::
          mkSchema(new UUIDSchema) ::
          Nil

      for {
        (from, to) <- customMappedTypes
        p <- props
      } {
        codeGen.getSchemaType(p(from)) shouldNot be(to)
      }
    }
  }

}