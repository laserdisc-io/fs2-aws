import sbt.*
import sbt.Keys.*

import java.io.*
import java.lang.reflect.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import scala.reflect.ClassTag

object TaglessGen {

  lazy val taglessGenDir     = settingKey[File]("directory where tagless-final algebras go")
  lazy val taglessGenPackage = settingKey[String]("package where tagless-final algebras go")
  lazy val taglessGenRenames = settingKey[Map[Class[?], String]]("map of imports that must be renamed")
  lazy val taglessGen        = taskKey[Seq[File]]("generate tagless-final algebras")
  lazy val taglessAwsService =
    taskKey[String]("What aws service generate algebras, must match the package name  ex [s3, sqs, sns, kinesis]")

  def taglessGenSettings[T](awsSvcId: String)(implicit ct: ClassTag[T]) = Seq(
    taglessGenDir     := (Compile / scalaSource).value / "io" / "laserdisc" / "pure" / awsSvcId / "tagless",
    taglessGenPackage := s"io.laserdisc.pure.$awsSvcId.tagless",
    taglessAwsService := awsSvcId,
    taglessGenRenames := Map(classOf[java.sql.Array] -> "SqlArray"),
    taglessGen        :=
      new TaglessGen[T](
        taglessGenPackage.value,
        taglessGenRenames.value,
        state.value.log,
        taglessAwsService.value
      ).gen(baseDirectory.value, taglessGenDir.value)
  )

}

class TaglessGen[T](
    pkg: String,
    renames: Map[Class[?], String],
    log: Logger,
    awsService: String
)(implicit ct: ClassTag[T]) {

  val clientFQDN: String       = ct.runtimeClass.getCanonicalName
  val clientSimpleName: String = ct.runtimeClass.getSimpleName

  // These Java classes will have non-Java names in our generated code
  val ClassBoolean: Class[Boolean] = classOf
  val ClassByte: Class[Byte]       = classOf
  val ClassShort: Class[Short]     = classOf
  val ClassInt: Class[Int]         = classOf
  val ClassLong: Class[Long]       = classOf
  val ClassFloat: Class[Float]     = classOf
  val ClassDouble: Class[Double]   = classOf
  val ClassObject: Class[Object]   = classOf
  val ClassVoid: Class[Void]       = Void.TYPE

  def tparams(t: Type): List[String] =
    t match {
      case t: GenericArrayType  => tparams(t.getGenericComponentType)
      case t: ParameterizedType => t.getActualTypeArguments.toList.flatMap(tparams)
      case t: TypeVariable[_]   => List(t.toString)
      case _                    => Nil
    }

  def toScalaType(t: Type): String =
    t match {
      case t: GenericArrayType  => s"Array[${toScalaType(t.getGenericComponentType)}]"
      case t: ParameterizedType =>
        s"${toScalaType(t.getRawType)}${t.getActualTypeArguments.map(toScalaType).mkString("[", ", ", "]")}"
      case t: WildcardType =>
        t.getUpperBounds.toList.filterNot(_ == classOf[Object]) match {
          case (c: Class[_]) :: Nil => s"_ <: ${c.getName}"
          case Nil                  => "_"
          case cs                   => sys.error("unhandled upper bounds: " + cs.toList)
        }
      case t: TypeVariable[_]       => t.toString
      case ClassVoid                => "Unit"
      case ClassBoolean             => "Boolean"
      case ClassByte                => "Byte"
      case ClassShort               => "Short"
      case ClassInt                 => "Int"
      case ClassLong                => "Long"
      case ClassFloat               => "Float"
      case ClassDouble              => "Double"
      case ClassObject              => "AnyRef"
      case x: Class[_] if x.isArray => s"Array[${toScalaType(x.getComponentType)}]"
      case x: Class[_]              => renames.getOrElse(x, x.getSimpleName)
    }

  // Each constructor for our algebra maps to an underlying method, and an index is provided to
  // disambiguate in cases of overloading.
  case class Ctor(method: Method, index: Int) {

    // The method name, unchanged
    def mname: String =
      method.getName

    // The case class constructor name, capitalized and with an index when needed
    def cname: String = {
      val s = mname(0).toUpper +: mname.drop(1)
      if (index == 0) s else s"$s$index"
    }

    // Constructor parameter type names
    def cparams: List[String] =
      method.getGenericParameterTypes.toList.map(toScalaType)

    def ctparams: String = {
      val ss = (method.getGenericParameterTypes.toList.flatMap(tparams) ++ tparams(
        method.getGenericReturnType
      )).toSet
      if (ss.isEmpty) "" else ss.mkString("[", ", ", "]")
    }

    // Constructor arguments, a .. z zipped with the right type
    def cargs: List[String] =
      "abcdefghijklmnopqrstuvwxyz".toList.zip(cparams).map { case (n, t) =>
        s"$n: $t"
      }

    // Return type name
    def ret: String =
      method.getGenericReturnType match {
        case t: ParameterizedType if t.getRawType == classOf[CompletableFuture[?]] =>
          toScalaType(t.getActualTypeArguments.toList.head)
        case t => toScalaType(t)
      }

    def kleisliRet(ret: String): String =
      s"Kleisli[M, $clientSimpleName, $ret]"

    def isRetFuture: Boolean =
      method.getGenericReturnType match {
        case t: ParameterizedType if t.getRawType == classOf[CompletableFuture[?]] => true
        case t                                                                     => false
      }

    // Case class/object declaration
    def ctor(opname: String): String =
      ((cparams match {
        case Nil => s"case object $cname"
        case ps  => s"final case class $cname$ctparams(${cargs.mkString(", ")})"
      }) + s""" extends $opname[$ret] {
              |      def visit[F[_]](v: Visitor[F]) = v.$mname${if (args.isEmpty) "" else s"($args)"}
              |    }""").trim.stripMargin

    // Argument list: a, b, c, ... up to the proper arity
    def args: String =
      "abcdefghijklmnopqrstuvwxyz".toList.take(cparams.length).mkString(", ")

    // Pattern to match the constructor
    def pat: String =
      cparams match {
        case Nil => s"object $cname"
        case ps  => s"class  $cname(${cargs.mkString(", ")})"
      }

    // Case clause mapping this constructor to the corresponding primitive action
    def prim(sname: String): String =
      if (cargs.isEmpty)
        s"case $cname => primitive(_.$mname)"
      else
        s"case $cname($args) => primitive(_.$mname($args))"

    // Smart constructor
    def lifted(ioname: String): String =
      if (cargs.isEmpty) {
        s"val $mname: $ioname[$ret] = FF.liftF($cname)"
      } else {
        s"def $mname$ctparams(${cargs.mkString(", ")}): $ioname[$ret] = FF.liftF($cname($args))"
      }

    def visitor: String =
      if (cargs.isEmpty) s"def $mname: F[$ret]"
      else s"def $mname$ctparams(${cargs.mkString(", ")}): F[$ret]"

    def stub: String =
      if (cargs.isEmpty) s"""def $mname: F[$ret] = sys.error("Not implemented: $mname")"""
      else
        s"""def $mname$ctparams(${cargs.mkString(
            ", "
          )}): F[$ret] = sys.error("Not implemented: $mname$ctparams(${cparams
            .mkString(", ")})")"""

    // Kleisli[M, KinesisAsyncClient, CreateStreamResponse]
    def kleisliImpl: String = {
      val ep = if (isRetFuture) "eff" else "primitive"
      if (cargs.isEmpty) s"override def $mname : ${kleisliRet(ret)} = $ep(_.$mname) // A"
      else
        s"override def $mname$ctparams(${cargs.mkString(", ")}): ${kleisliRet(ret)} = $ep(_.$mname($args)) // B"
    }

    def kleisliLensImpl: String = {
      val ep = if (isRetFuture) "eff1" else "primitive1"
      if (cargs.isEmpty) s"override def $mname = Kleisli(e => $ep(f(e).$mname))"
      else
        s"override def $mname$ctparams(${cargs.mkString(", ")}) = Kleisli(e => $ep(f(e).$mname($args)))"
    }

    def fImpl: String = {
      val ep = if (isRetFuture) "eff1" else "primitive1"
      if (cargs.isEmpty) s"override def $mname = $ep(client.$mname)"
      else
        s"override def $mname$ctparams(${cargs.mkString(", ")}) = $ep(client.$mname($args))"
    }

  }

  // This class, plus any superclasses and interfaces, "all the way up"
  def closure(c: Class[?]): List[Class[?]] =
    (c :: (Option(c.getSuperclass).toList ++ c.getInterfaces.toList).flatMap(closure)).distinct
      .filterNot(_.getName == "java.lang.AutoCloseable") // not available in jdk1.6
      .filterNot(_.getName == "java.lang.Object")        // we don't want .equals, etc.

  implicit class MethodOps(m: Method) {
    def isStatic: Boolean =
      (m.getModifiers & Modifier.STATIC) != 0
  }

  // All non-deprecated methods for this class and any superclasses/interfaces
  def methods(c: Class[?]): List[Method] =
    closure(c)
      .flatMap(_.getDeclaredMethods.toList)
      .distinct
      .filterNot(_.isStatic)
      .filter(_.getAnnotation(classOf[Deprecated]) == null)
      .filterNot(_.getParameterTypes.contains(classOf[Consumer[?]]))

  // Ctor values for all methods in of A plus superclasses, interfaces, etc.
  def ctors: List[Ctor] =
    methods(ct.runtimeClass)
      .groupBy(_.getName)
      .toList
      .flatMap { case (n, ms) =>
        ms
          .sortBy(_.getGenericParameterTypes.map(toScalaType).mkString(","))
          .zipWithIndex
          .map { case (m, i) =>
            Ctor(m, i)
          }
      }
      .filter(_.mname != "serviceClientConfiguration") // multiple instances with varying return signatures
      .sortBy(c => (c.mname, c.index))

  // Fully qualified rename, if any
  def renameImport(c: Class[?]): String = {
    val sn = c.getSimpleName
    val an = renames.getOrElse(c, sn)
    if (sn == an) c.getName
    else s"${c.getPackage.getName}.{ $sn => $an }"
  }

  // All types referenced by all methods on A, superclasses, interfaces, etc.
  def referenceImports: List[String] = {

    def extractGenericTypes(m: Method): List[Class[?]] = m.getGenericReturnType match {
      case t: ParameterizedType if t.getRawType == classOf[CompletableFuture[?]] =>
        m.getReturnType :: Nil // t.getActualTypeArguments.toList.map(tt => Class.forName(tt.getTypeName))
      case _ => m.getReturnType :: Nil
    }

    ctors
      .map(_.method)
      .flatMap(m => m.getReturnType :: m.getParameterTypes.toList)
      .map(t => if (t.isArray) t.getComponentType else t)
      .filterNot(t => t.isPrimitive || t == classOf[Object])
      .map(c => renameImport(c))
      .filterNot(c =>
          c.matches("java.lang.*") ||
          c.matches("java.util.concurrent.CompletableFuture") ||
          c.matches(s"software\\.amazon\\.awssdk\\.services\\.$awsService\\.model\\.[^.]+$$")
      )
      .distinct.sorted
      .map(c => s"import $c")

  }

  // The algebra module for T
  val module: String = {
    val oname  = ct.runtimeClass.getSimpleName // original name, without name mapping
    val sname  = toScalaType(ct.runtimeClass)
    val opname = s"${oname}Op"
    s"""
    |package $pkg
    |
    |import software.amazon.awssdk.services.$awsService.model.*
    |
    |${referenceImports.mkString("\n")}
    |
    |/**
    | * The effectful equivalents for operations detected from [[$clientFQDN]]
    | */
    |trait $opname[F[_]] {
    |
    |${ctors.map(_.visitor).mkString("\t", "\n\t", "\n")}
    |
    |}
    |""".trim.stripMargin
  }

  // Import for the IO type for a carrer type, with renaming
  def ioImport(c: Class[?]): String = {
    val sn = c.getSimpleName
    s"import ${sn.toLowerCase}.${sn}IO"
  }

  val kleisliInterp: String = {
    val oname = ct.runtimeClass.getSimpleName // original name, without name mapping
    val sname = toScalaType(ct.runtimeClass)
    s"""
       |trait ${oname}Interpreter extends ${oname}Op[Kleisli[M, $sname, *]] {
       |
       |  // domain-specific operations are implemented in terms of `primitive`
       |${ctors.map(_.kleisliImpl).mkString("\n").indent(2)}
       |
       |  def lens[E](f: E => $oname): ${oname}Op[Kleisli[M, E, *]] =
       |    new ${oname}Op[Kleisli[M, E, *]] {
       |${ctors.map(_.kleisliLensImpl).mkString("\n").indent(4)}
       |    }
       |  }
       |""".trim.stripMargin
  }

  val fInterp: String = {
    val oname = ct.runtimeClass.getSimpleName // original name, without name mapping
    s"""
       |  def ${oname}Resource(builder : ${oname}Builder) : Resource[M, $oname] = Resource.fromAutoCloseable(asyncM.delay(builder.build()))
       |  def ${oname}OpResource(builder: ${oname}Builder) = ${oname}Resource(builder).map(create)
       |
       |  def create(client : $oname) : ${oname}Op[M] = new ${oname}Op[M] {
       |
       |    // domain-specific operations are implemented in terms of `primitive`
       |${ctors.map(_.fImpl).mkString("\n").indent(4)}
       |
       |  }
       |""".trim.stripMargin
  }

  val interpreterDef: String = {
    val oname  = ct.runtimeClass.getSimpleName // original name, without name mapping
    val sname  = toScalaType(ct.runtimeClass)
    val ioname = s"${oname}IO"
    val mname  = oname.toLowerCase
    s"lazy val ${oname}Interpreter: ${oname}Interpreter = new ${oname}Interpreter { }"
  }

  // template for a kleisli interpreter
  val kleisliInterpreter: String =
    s"""
       |package $pkg
       |
       |// Library imports
       |import cats.data.Kleisli
       |import cats.effect.{ Async,  Resource }
       |
       |import software.amazon.awssdk.services.$awsService.*
       |import software.amazon.awssdk.services.$awsService.model.*
       |
       |// Types referenced
       |${referenceImports.distinct.sorted.mkString("\n")}
       |
       |
       |object Interpreter {
       |
       |  def apply[M[_]](
       |    implicit am: Async[M]
       |  ): Interpreter[M] =
       |    new Interpreter[M] {
       |      val asyncM = am
       |    }
       |
       |}
       |
       |// Family of interpreters into Kleisli arrows for some monad M.
       |trait Interpreter[M[_]] { outer =>
       |
       |  import java.util.concurrent.CompletableFuture
       |
       |  implicit val asyncM: Async[M]
       |
       |  $interpreterDef
       |  // Some methods are common to all interpreters and can be overridden to change behavior globally.
       |
       |  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(j => asyncM.blocking(f(j)))
       |  def primitive1[J, A](f: => A): M[A]              = asyncM.blocking(f)
       |
       |  def eff[J, A](fut: J => CompletableFuture[A]): Kleisli[M, J, A] = Kleisli { j =>
       |    asyncM.fromCompletableFuture(asyncM.delay(fut(j)))
       |  }
       |  def eff1[J, A](fut: => CompletableFuture[A]): M[A] =
       |    asyncM.fromCompletableFuture(asyncM.delay(fut))
       |
       |  // Interpreters
       |${kleisliInterp.indent(2)}
       |  // end interpreters
       |
       |$fInterp
       |
       |}
       |""".trim.stripMargin

  def gen(projectbase: File, base: File): Seq[java.io.File] = {

    def writeFile(fileName: String, content: String): File = {
      val file = new File(base, fileName)
      val pw   = new PrintWriter(file)
      pw.println(content)
      pw.close()
      file
    }

    base.mkdirs()

    val op = writeFile(s"${clientSimpleName}Op.scala", module);
    val ki = writeFile(s"Interpreter.scala", kleisliInterpreter);

    log.info(s"""Generating Tagless Algebra for $awsService:
        |   SDK Client: $clientSimpleName
        |       Output: ${base.relativeTo(projectbase).getOrElse(base)}
        |    Generated: ${op.getName}, impl:${ki.getName}
        |
        |""".stripMargin)

    List(op, ki)
  }

}
