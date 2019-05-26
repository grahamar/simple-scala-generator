package io.grhodes.simple.codegen

import java.io.File
import java.util
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

import io.swagger.codegen.v3.generators.DefaultCodegenConfig
import io.swagger.codegen.v3.{CodegenConfig, CodegenConstants, CodegenOperation, CodegenProperty}
import io.swagger.codegen.v3.generators.scala.AbstractScalaCodegen
import io.swagger.v3.oas.models.media.{Schema, StringSchema}
import org.apache.commons.lang3.StringUtils

import scala.collection.JavaConverters._

object BaseScalaCodegen {
  val ARG_INCLUDE_SERIALIZATION = "includeSerialization"
  val NUMBER_TYPES = Set("Int", "Long", "Float", "Double")

  def camelize(parts: Array[String]): String = {
    val sb = new StringBuilder()
    parts.foreach(s => sb.append(StringUtils.capitalize(s)))
    sb.toString()
  }
}

abstract class BaseScalaCodegen extends AbstractScalaCodegen with CodegenConfig {
  import BaseScalaCodegen._

  protected val invokerPackage = new AtomicReference[String]("swagger.models")

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
              val replacement = ":" + DefaultCodegenConfig.camelize(mtch.group(1), true)
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
