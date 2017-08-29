package services.execution

import scala.xml.{Node, PrettyPrinter}

/**
 * Created by Sergey Tarkhanov on 6/3/2015.
 */
case class SchemaElement(name: String, typeDef: Either[String, SchemaType], minOccurs: Option[Int] = None, maxOccurs: Option[Int] = None)
case class SchemaAttribute(name: String, typeDef: Either[String, SchemaType], required: Boolean)
case class SchemaType(name: Option[String], elements: List[SchemaElement], attributes: List[SchemaAttribute] = List())

object RulesWSDLDoc {

  private def location(serviceFilter: String) = s"https://localhost/interface/rules/$serviceFilter/soap"

  private val serviceName = "Rules"
  private val targetNamespace = "http://example.com/rules/"

  private val printer = new PrettyPrinter(200, 2)

  def create(serviceFilter: String, elements: List[SchemaElement], types: List[SchemaType], documentation: String = "") = {
    val xml = printer.format(wsdl(serviceFilter, elements, types, documentation))
    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + xml
  }

  private def renderType(t: SchemaType): Node =
    <xsd:complexType name={t.name.orNull}>
      <xsd:sequence>
        {t.elements.map(renderElement)}
      </xsd:sequence>
      { t.attributes.map(renderAttribute)}
    </xsd:complexType>

  private def typeNamespace(name: String): String =
    name match {
      case "string" | "int" | "boolean" | "anyType"  => "xsd:"
      case _ => "tns:"
    }

  private def maxOccursValue(value: Option[Int]): String =
    value.map(v => if (v == Int.MaxValue) "unbounded" else v.toString).orNull

  private def minOccursValue(value: Option[Int]): String =
    value.map(_.toString).orNull

  private def renderElement(e: SchemaElement): Node =
    e.typeDef match {
      case Left(typeName) => <xsd:element name={e.name} type={typeNamespace(typeName) + typeName} maxOccurs={maxOccursValue(e.maxOccurs)} minOccurs={minOccursValue(e.minOccurs)}/>
      case Right(typeDef) => <xsd:element name={e.name} maxOccurs={maxOccursValue(e.maxOccurs)} minOccurs={minOccursValue(e.minOccurs)}>
        {renderType(typeDef)}
      </xsd:element>
    }

  private def renderAttribute(e: SchemaAttribute): Node =
    e.typeDef match {
      case Left(typeName) => <xsd:attribute name={e.name} type={typeNamespace(typeName) + typeName} use={if (e.required) "required" else "optional"}/>
      case Right(typeDef) => <xsd:attribute name={e.name} use={if (e.required) "required" else "optional"}>
        {renderType(typeDef)}
      </xsd:attribute>
    }

  private def schema(elements: List[SchemaElement], types: List[SchemaType]) =
    <xsd:schema targetNamespace={targetNamespace}>
      {elements.map(renderElement)}{types.map(renderType)}
      <xsd:element name="rulesFault">
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element name="code" type="xsd:int" maxOccurs="1" minOccurs="1" />
            <xsd:element name="message" type="xsd:string" maxOccurs="1" minOccurs="1" />
          </xsd:sequence>
        </xsd:complexType>
      </xsd:element>
    </xsd:schema>

  private def wsdl(serviceFilter: String, elements: List[SchemaElement], types: List[SchemaType], documentation: String) =
    <definitions xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/" xmlns:tns={targetNamespace} xmlns="http://schemas.xmlsoap.org/wsdl/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" name={serviceName} targetNamespace={targetNamespace}>
      <types>
        {schema(elements, types)}
      </types>
      <message name="rulesRequest">
        <part element="tns:rulesRequest" name="parameters"/>
      </message>
      <message name="rulesResponse">
        <part element="tns:rulesResponse" name="parameters"/>
      </message>
      <message name="rulesFault">
        <part element="tns:rulesFault" name="parameters" />
      </message>
      <portType name={serviceName}>
        <operation name="invoke">
          <input message="tns:rulesRequest"/>
          <output message="tns:rulesResponse"/>
          <fault name="fault" message="tns:rulesFault" />
        </operation>
      </portType>
      <binding name={serviceName + "SOAP"} type={"tns:" + serviceName}>
        <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
        <operation name="invoke">
          <soap:operation soapAction={targetNamespace + "invoke"}/>
          <input>
            <soap:body use="literal"/>
          </input>
          <output>
            <soap:body use="literal"/>
          </output>
        </operation>
      </binding>
      <service name={serviceName}>
        <documentation>{documentation}</documentation>
        <port binding={"tns:" + serviceName + "SOAP"} name={serviceName + "SOAP"}>
          <soap:address location={location(serviceFilter)}/>
        </port>
      </service>
    </definitions>

}
