package models.repository.types

import scala.collection.Seq
import scala.xml.{Node, NodeSeq}
import models.repository.types.TypesModel._

/**
 * Created by Sergey Tarkhanov on 5/3/2015.
 */
object TypeModelXML {

  def readTypes(root: Node): List[Type] =
    (root \ "type").flatMap(nodeToType).toList

  private def nodeToType(typeDef: Node): Seq[Type] = {
    val name = typeDef \ "@name"
    if (name.nonEmpty) {
      val seq = typeDef.attribute("seq").map(_.head.text)
      Seq(Type(seq, name.head.text, readFields(typeDef)))
    }
    else
      Nil
  }

  def readFields(root: Node): List[Field] =
    (root \ "property").flatMap(nodeToField).toList

  private def nodeToField(fieldDef: Node): Seq[Field] = {
    val name: NodeSeq = fieldDef \ "@name"
    val fieldType = fieldDef \ "@type"
    if (name.nonEmpty && fieldType.nonEmpty)
      Seq(Field(name.head.text, fieldType.head.text))
    else
      Nil
  }

  // ---------------------------------------------------

  def serialize(tp: Type): String =
    <type>
      {tp.fields.map(f => <property name={f.name} type={f.typeDef}/>)}
    </type>.toString()

}
