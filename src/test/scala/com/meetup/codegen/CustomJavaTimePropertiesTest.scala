package com.meetup.codegen

import io.swagger.models.properties._
import org.scalatest.{FunSpec, Matchers}

class CustomJavaTimePropertiesTest extends FunSpec with Matchers {

  val codeGen = TestScalaCodegen.getServer

  val customMappedTypes =
    Map(
      "timestamp" -> "Instant",
      "local-time" -> "LocalTime",
      "local-date-time" -> "LocalDateTime"
    )

  describe("a custom date/time property") {
    it("should map to the expected type") {
      customMappedTypes.foreach {
        case (from, to) =>
          val prop = mkProp(new StringProperty)(from)
          codeGen.getSwaggerType(prop) shouldBe to
      }
    }

    it("should not be mapped from any property type other than string") {
      val props: List[String => Property] =
        mkProp(new BinaryProperty) ::
          mkProp(new BooleanProperty) ::
          mkProp(new ByteArrayProperty) ::
          mkProp(new DateProperty) ::
          mkProp(new DateTimeProperty) ::
          mkProp(new DoubleProperty) ::
          mkProp(new FloatProperty) ::
          mkProp(new EmailProperty) ::
          mkProp(new FileProperty) ::
          mkProp(new IntegerProperty) ::
          mkProp(new LongProperty) ::
          mkProp(new MapProperty) ::
          mkProp(new ObjectProperty) ::
          mkProp(new PasswordProperty) ::
          mkProp(new RefProperty("dummy_ref")) ::
          mkProp(new UUIDProperty) ::
          Nil

      for {
        (from, to) <- customMappedTypes
        p <- props
      } {
        codeGen.getSwaggerType(p(from)) shouldNot be(to)
      }
    }
  }

}