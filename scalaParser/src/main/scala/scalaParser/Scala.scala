package scalaParser
import acyclic.file
import language.implicitConversions
import syntax._
import parsing._
/**
 * Parser for Scala syntax.
 */
object Scala extends Core with Types with Exprs/* with Xml*/{

  private implicit def wspStr(s: String) = WL ~ s


  val TmplBody: R0 = {
    val Prelude = R( (Annot ~ OneNLMax).rep ~ Mod.rep )
    val DefDcl = R(
      `def` ~! FunSig ~ FunDcl ~ FunBody.? |
      `type` ~! (TypeDef | TypeDcl) |
      `val` ~! (ValDef | ValVarDcl) |
      `var` ~! (VarDef | ValVarDcl) |
      TraitDef | ClsDef | ObjDef
    )
    val TmplStat = R( Import | Prelude ~ DefDcl | StatCtx.Expr )
    val SelfType = R( (`this` | Id | `_`) ~ (`:` ~ InfixType).? ~ `=>` )
    R( "{" ~ SelfType.? ~ Semis.? ~ TmplStat.rep(Semis) ~ `}` )
  }

  val NewBody = R( ClsTmpl | TmplBody )

  val ValDef = R( Pat2.rep1(",") ~ (`:` ~ Type).? ~ `=` ~ StatCtx.Expr )
  val VarDef = R( Ids ~ `:` ~ Type ~ `=` ~ `_` | ValDef )

  val FunBody = R( `=` ~ `macro`.? ~ StatCtx.Expr | OneNLMax ~ "{" ~ Block ~ "}" )
  val FunDef = R( FunDcl ~ FunBody )


  val BlockDef: R0 = R( `def` ~ FunSig ~ FunDef | `type` ~ TypeDef | `val` ~ ValDef | `var` ~ VarDef | TraitDef | ClsDef | ObjDef )

  val ClsDef = {
    val ClsAnnot = R( `@` ~! SimpleType ~ ArgList )
    val Prelude = R( NotNewline ~ ( ClsAnnot.rep1 ~ AccessMod.? | ClsAnnot.rep ~ AccessMod) )
    val ClsArgMod = R( (Mod.rep ~ (`val` | `var`)).? )
    val ClsArg = R( Annot.rep ~ ClsArgMod ~ Id ~! `:` ~ ParamType ~ (`=` ~ ExprCtx.Expr).? )

    val Implicit = R( OneNLMax ~ "(" ~ `implicit` ~ ClsArg.rep1(",") ~ ")" )
    val ClsArgs = R( OneNLMax ~ "(" ~ ClsArg.rep(",") ~ ")" )
    val AllArgs = R( ClsArgs.rep1 ~ Implicit.? | Implicit )
    R( `case`.? ~ `class` ~! Id ~ TypeArgList.? ~ Prelude.? ~ AllArgs.? ~ ClsTmplOpt )
  }
  val TraitDef = {
    val TraitTmplOpt = {
      val TraitParents = R( AnnotType ~ (`with` ~ AnnotType).rep )
      val TraitTmpl = R( EarlyDefs.? ~ TraitParents ~ TmplBody.? )
      R( `extends` ~ TraitTmpl | `extends`.? ~ TmplBody )
    }
    R( `trait` ~! Id ~ TypeArgList.? ~ TraitTmplOpt.? )
  }

  val ObjDef: R0 = R( `case`.? ~ `object` ~! Id ~ ClsTmplOpt )
  val ClsTmplOpt: R0 = R( `extends` ~ ClsTmpl | (`extends`.? ~ TmplBody).? )

  val ClsTmpl: R0 = {
    val Constr = R( AnnotType ~ (NotNewline ~ ArgList).rep )
    val ClsParents = R( Constr ~ (`with` ~ AnnotType).rep )
    R( EarlyDefs.? ~ ClsParents ~ TmplBody.? )
  }

  val EarlyDefs: R0 = {
    val EarlyDef = R( (Annot ~ OneNLMax).rep ~ Mod.rep ~ ((`val` ~ ValDef) | (`var` ~ VarDef)) )
    R( `{` ~ EarlyDef.rep(Semis) ~ `}` ~ `with` )
  }

  val PkgObj = R( ObjDef )
  val PkgBlock = R( QualId ~! `{` ~ TopStatSeq.? ~ `}` )
  val TopStatSeq: R0 = {
    val Tmpl = R( (Annot ~ OneNLMax).rep ~ Mod.rep ~ (TraitDef | ClsDef | ObjDef) )
    val TopStat = R( `package` ~ (PkgBlock | PkgObj) | Import | Tmpl )
    R( TopStat.rep1(Semis) )
  }
  val TopPkgSeq = R( (`package` ~ QualId ~ !(WS ~ "{")).rep1(Semis) )
  val CompilationUnit: R0 = {
    val Body = R( TopPkgSeq ~ (Semis ~ TopStatSeq).? | TopStatSeq )
    R( Semis.? ~ Body.? ~ Semis.? ~ WL ~ Parser.End)
  }
}
