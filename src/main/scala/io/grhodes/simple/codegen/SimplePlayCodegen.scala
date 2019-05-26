package io.grhodes.simple.codegen

import java.io.File
import java.util
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

import io.swagger.codegen._
import io.swagger.models.properties.{ArrayProperty, MapProperty, Property, StringProperty}
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConverters._

object SimplePlayCodegen {
  val ARG_RESOURCE_OUTPUT_DIRECTORY = "resourceOutputDirectory"
  val ARG_INCLUDE_SERIALIZATION = "includeSerialization"
  val NUMBER_TYPES = Set("Int", "Long", "Float", "Double")

  def camelize(parts: Array[String]): String = {
    val sb = new StringBuilder()
    parts.foreach(s => sb.append(StringUtils.capitalize(s)))
    sb.toString()
  }

}

class SimplePlayCodegen extends DefaultCodegen with CodegenConfig {
  import SimplePlayCodegen._

  override def getHelp(): String = s"Generates ${getName()} library."
  override def getName(): String = "simple-play"
  override def getTag(): CodegenType = CodegenType.CLIENT

  val invokerPackage = new AtomicReference[String]("swagger.models")
  val invokerFolder = new AtomicReference[String](invokerPackage.get.replace(".", "/"))

  {
    outputFolder = "generated-code/" + getName()
    cliOptions.add(CliOption.newBoolean(ARG_INCLUDE_SERIALIZATION, "To include or not include serializers in the model classes"))
    cliOptions.add(CliOption.newBoolean(ARG_RESOURCE_OUTPUT_DIRECTORY, "The output resource directory"))

    /*
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "simple-play"

    /*
     * Api Package. Optional, if needed, this can be used in templates
     */
    apiPackage = "swagger.api"

    val resourceOutDir = Option(additionalProperties.get(ARG_RESOURCE_OUTPUT_DIRECTORY)).map(_.toString).getOrElse("conf")
    supportingFiles = List(
      new SupportingFile("routes.mustache", resourceOutDir, "generated.routes")
    ).asJava

    /*
     * Model Package.  Optional, if needed, this can be used in templates
     */
    modelPackage = "swagger.models"

    reservedWords = Set(
      "abstract", "case", "catch", "class", "def", "do", "else", "extends",
      "false", "final", "finally", "for", "forSome", "if", "implicit",
      "import", "lazy", "match", "new", "null", "object", "override",
      "package", "private", "protected", "return", "sealed", "super",
      "this", "throw", "trait", "try", "true", "type", "val", "var",
      "while", "with", "yield"
    ).asJava

    languageSpecificPrimitives = Set(
      "Boolean",
      "Double",
      "Float",
      "Int",
      "Long",
      "List",
      "Map",
      "String"
    ).asJava

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

    val givenModelPkg = Option(additionalProperties.get(CodegenConstants.MODEL_PACKAGE)).map(_.toString)
    modelPackage = givenModelPkg
      .orElse(Option(invokerPackage.get()).filter(_.nonEmpty).map(_  + ".model"))
      .getOrElse("model")

    val givenApiPkg = Option(additionalProperties.get(CodegenConstants.API_PACKAGE)).map(_.toString)
    apiPackage = givenApiPkg
      .orElse(Option(invokerPackage.get()).filter(_.nonEmpty).map(_  + ".api"))
      .getOrElse("api")

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

  override def postProcessOperations(objs: util.Map[String, AnyRef]): util.Map[String, AnyRef] = {
    val ops = Option(objs.get("operations")).map(_.asInstanceOf[util.Map[String, Object]].asScala)
    ops.map { operations =>
      val op = operations.get("operation").collect {
        case operation: util.List[CodegenOperation] =>
          operation.asScala.map { op =>
            val pathVariableMatcher = Pattern.compile("\\{([^}]+)}")
            val mtch = pathVariableMatcher.matcher(op.path)
            while (mtch.find()) {
              val completeMatch = mtch.group()
              val replacement = ":" + DefaultCodegen.camelize(mtch.group(1), true)
              op.path = op.path.replace(completeMatch, replacement)
            }
            op
          }
        case other => other
      }
      mapAsJavaMapConverter(operations + ("operation" -> op)).asJava
    }.getOrElse(new util.HashMap[String, Object]())
  }

  /**
    * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
    * those terms here.  This logic is only called if a variable matches the reseved words
    *
    * @return the escaped term
    */
  override def escapeReservedWord(name: String): String = s"`$name`"

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
    * Optional - type declaration.  This is a String which is used by the templates to instantiate your
    * types.  There is typically special handling for different property types
    *
    * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
    */
  override def getTypeDeclaration(p: Property): String = p match {
    case ap: ArrayProperty => s"${getSwaggerType(p)}[${getTypeDeclaration(ap.getItems)}]"
    case mp: MapProperty => s"${getSwaggerType(p)}[String, ${getTypeDeclaration(mp.getAdditionalProperties)}]"
    case _ => super.getTypeDeclaration(p)
  }

  /**
    * Optional - swagger type conversion.  This is used to map swagger types in a `Property` into
    * either language specific types via `typeMapping` or into complex models if there is not a mapping.
    *
    * @return a string value of the type or complex model for this property
    * @see io.swagger.models.properties.Property
    */
  override def getSwaggerType(p: Property): String = {
    val types = Set("date", "date-time", "timestamp", "local-time", "local-date-time")

    val swaggerType = if (p.getClass.equals(classOf[StringProperty]) && types.contains(p.getFormat)) {
      p.getFormat
    } else {
      super.getSwaggerType(p)
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
