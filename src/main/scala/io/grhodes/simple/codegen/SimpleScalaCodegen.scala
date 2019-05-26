package io.grhodes.simple.codegen

import java.io.File
import java.util
import java.util.concurrent.atomic.AtomicReference

import io.swagger.codegen.v3._
import io.swagger.codegen.v3.generators.scala.AbstractScalaCodegen
import io.swagger.v3.oas.models.media.{Schema, StringSchema}
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConverters._

object SimpleScalaCodegen {
  val ARG_INCLUDE_SERIALIZATION = "includeSerialization"
  val NUMBER_TYPES = Set("Int", "Long", "Float", "Double")

  def camelize(parts: Array[String]): String = {
    val sb = new StringBuilder()
    parts.foreach(s => sb.append(StringUtils.capitalize(s)))
    sb.toString()
  }

}

class SimpleScalaCodegen extends AbstractScalaCodegen with CodegenConfig {
  import SimpleScalaCodegen._

  override def getHelp(): String = s"Generates ${getName()} library."
  override def getDefaultTemplateDir: String = getName()
  override def getName(): String = "simple-scala"
  override def getTag(): CodegenType = CodegenType.CLIENT

  val invokerPackage = new AtomicReference[String]("swagger.models")
  val invokerFolder = new AtomicReference[String](invokerPackage.get.replace(".", "/"))

  {
    outputFolder = "generated-code/" + getName()
    cliOptions.add(CliOption.newBoolean(ARG_INCLUDE_SERIALIZATION, "To include or not include serializers in the model classes"))

    /*
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "simple-scala"

    /*
     * Api Package. Optional, if needed, this can be used in templates
     */
    apiPackage = "swagger.api"

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

  /*
    * Lifted from https://github.com/swagger-api/swagger-codegen/blob/master/modules/swagger-codegen/src/main/java/io/swagger/codegen/languages/AbstractScalaCodegen.java#L182-L185
    * to prevent injection.
    */
  override def escapeUnsafeCharacters(input: String): String = input.replace("*/", "*_/").replace("/*", "/_*")

  override def escapeQuotationMark(input: String): String = input.replace("\"", "\\\"")

  override def processOpts(): Unit = {
    val givenInvPkg = additionalProperties.get(CodegenConstants.INVOKER_PACKAGE)
    Option(givenInvPkg).foreach(pkg => invokerPackage.set(pkg.toString))
    super.processOpts()
    additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage.get())

    modelPackage = Option(invokerPackage.get()).filter(_.nonEmpty).map(_  + ".model").getOrElse("model")
    apiPackage = Option(invokerPackage.get()).filter(_.nonEmpty).map(_  + ".api").getOrElse("api")

    invokerFolder.set(invokerPackage.get().replace(".", "/"))

    val incSer = additionalProperties.get(ARG_INCLUDE_SERIALIZATION)
    val includeSerialization: java.lang.Boolean = Option(incSer).forall(java.lang.Boolean.TRUE.toString.equals(_))
    additionalProperties.put(ARG_INCLUDE_SERIALIZATION, includeSerialization)
  }

  override def postProcessModels(objs: util.Map[String, AnyRef]): util.Map[String, AnyRef] = {
    val imports = objs.get("imports").asInstanceOf[util.List[util.Map[String, String]]].asScala
    val prefix = Option(modelPackage()).filter(_.nonEmpty).map(_  + ".").getOrElse("")
    val objects = Map(objs.asScala.toSeq: _*) ++ Map(
      "imports" -> imports.filterNot(_.get("import").startsWith(prefix)).asJava
    )
    // Now subject the models to Enum treatment.
    postProcessModelsEnum(objects.asJava)
  }

  /**
    * Location to write model files.  You can use the modelPackage() as defined when the class is
    * instantiated
    */
  override def modelFileFolder() = s"$outputFolder/${modelPackage().replace('.', File.separatorChar)}"

  /**
    * Location to write api files.  You can use the apiPackage() as defined when the class is
    * instantiated
    */
  override def apiFileFolder(): String = s"$outputFolder/${apiPackage().replace('.', File.separatorChar)}"

  /**
    * Optional - swagger type conversion.  This is used to map swagger types in a `Property` into
    * either language specific types via `typeMapping` or into complex models if there is not a mapping.
    *
    * @return a string value of the type or complex model for this property
    * @see io.swagger.models.properties.Property
    */
  override def getSchemaType(p: Schema[_]): String = {
    val types = Set("date", "date-time", "timestamp", "local-time", "local-date-time")

    val swaggerType = if (p.getClass.equals(classOf[StringSchema]) && types.contains(p.getFormat)) {
      p.getFormat
    } else {
      super.getSchemaType(p)
    }

    val typ = if (typeMapping.containsKey(swaggerType)) {
      typeMapping.get(swaggerType)
    } else {
      swaggerType
    }

    toModelName(typ)
  }

  override def toEnumName(property: CodegenProperty): String = camelize(property.name.split("_"))

  override def toEnumVarName(value: String, datatype: String): String = {
    if (NUMBER_TYPES.contains(datatype)) {
      "Number" + value
    } else {
      camelize(value.split("[ _]"))
    }
  }

  override def toEnumValue(value: String, datatype: String): String = {
    if (NUMBER_TYPES.contains(datatype)) {
      value
    } else {
      s""""${escapeText(value).toLowerCase()}""""
    }
  }

}
