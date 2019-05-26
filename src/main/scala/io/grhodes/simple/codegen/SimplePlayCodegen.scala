package io.grhodes.simple.codegen

import io.swagger.codegen.v3._

import scala.collection.JavaConverters._

class SimplePlayCodegen extends BaseScalaCodegen {
  import BaseScalaCodegen._

  override def getHelp(): String = s"Generates ${getName()} library."
  override def getDefaultTemplateDir: String = getName()
  override def getTag(): CodegenType = CodegenType.CLIENT
  override def getName(): String = "simple-play"

  {
    cliOptions.add(CliOption.newBoolean(ARG_INCLUDE_SERIALIZATION, "To include or not include serializers in the model classes"))
    cliOptions.add(CliOption.newString(ARG_SRC_MANAGED_DIRECTORY, "The managed source directory"))

    /*
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "simple-play"

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
    typeMapping.put("date", "java.time.LocalDate")
    typeMapping.put("Date", "java.time.LocalDate")
    typeMapping.put("timestamp", "java.time.Instant")
    typeMapping.put("datetime", "java.time.ZonedDateTime")
    typeMapping.put("date-time", "java.time.ZonedDateTime")
    typeMapping.put("DateTime", "java.time.ZonedDateTime")
    typeMapping.put("local-time", "java.time.LocalTime")
    typeMapping.put("local-date-time", "java.time.LocalDateTime")

    importMapping.remove("Set")
    importMapping.remove("Seq")
    importMapping.remove("List")
    importMapping.remove("Set")
    importMapping.remove("Map")

    modelTemplateFiles.put("model.mustache", ".scala")
  }

}
