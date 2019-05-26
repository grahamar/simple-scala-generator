package io.grhodes.simple.codegen

import io.swagger.codegen.v3._

import scala.collection.JavaConverters._

object SimplePlayCodegen {
  val ARG_SRC_MANAGED_DIRECTORY = "sourceManagedDir"
}

class SimplePlayCodegen extends BaseScalaCodegen {
  import SimplePlayCodegen._
  import BaseScalaCodegen._

  override def getHelp(): String = s"Generates ${getName()} library."
  override def getDefaultTemplateDir: String = getName()
  override def getTag(): CodegenType = CodegenType.CLIENT
  override def getName(): String = "simple-play"

  {
    outputFolder = "generated-code/" + getName()
    cliOptions.add(CliOption.newBoolean(ARG_INCLUDE_SERIALIZATION, "To include or not include serializers in the model classes"))
    cliOptions.add(CliOption.newString(ARG_SRC_MANAGED_DIRECTORY, "The managed source directory"))

    /*
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "simple-play"

    val managedSrcOpt = Option(additionalProperties.get(ARG_SRC_MANAGED_DIRECTORY)).map(_.toString)
    managedSrcOpt.foreach { managedSrc =>
      sourceFolder = managedSrc.substring(outputFolder.length)
    }

    /*
     * Api Package. Optional, if needed, this can be used in templates
     */
    apiPackage = "swagger.api"

    supportingFiles = List(
      new SupportingFile("routes.mustache", "conf", "generated.routes")
    ).asJava

    /*
     * Model Package.  Optional, if needed, this can be used in templates
     */
    modelPackage = "swagger.models"

    additionalProperties.put("java8", "true")
    additionalProperties.put("jsr310", "true")

    instantiationTypes.put("array", "List")
    instantiationTypes.put("integer", "Int")

    typeMapping.put("int", "Int")
    typeMapping.put("Integer", "Int")
    typeMapping.put("date", "LocalDate")
    typeMapping.put("Date", "LocalDate")
    typeMapping.put("timestamp", "Instant")
    typeMapping.put("datetime", "ZonedDateTime")
    typeMapping.put("date-time", "ZonedDateTime")
    typeMapping.put("DateTime", "ZonedDateTime")
    typeMapping.put("local-time", "LocalTime")
    typeMapping.put("local-date-time", "LocalDateTime")

    importMapping.remove("Set")
    importMapping.remove("Seq")
    importMapping.remove("List")
    importMapping.remove("Set")
    importMapping.remove("Map")

    importMapping.put("LocalTime", "java.time.LocalTime")
    importMapping.put("Instant", "java.time.Instant")
    importMapping.put("LocalDate", "java.time.LocalDate")
    importMapping.put("ZonedDateTime", "java.time.ZonedDateTime")
    importMapping.put("LocalDateTime", "java.time.LocalDateTime")
    importMapping.put("OffsetDateTime", "java.time.OffsetDateTime")

    modelTemplateFiles.put("model.mustache", ".scala")
  }

}
