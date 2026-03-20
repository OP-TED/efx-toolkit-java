<?xml version="1.0" encoding="utf-8" ?>
<#--
  Template for complete-validation.sch master file.

  Parameters:
    title           - Schema title (e.g., "eForms validation (dynamic)")
    params          - List<SchematronParam> of API endpoint parameters
    letElements     - List<SchematronLet> of schema-level let element declarations
    phases          - List<SchematronPhase> defining validation phases per notice type
    diagnostics     - List<SchematronDiagnostic> for subject path information
    includes        - List<String> of pattern file paths to include
-->
<schema xmlns="http://purl.oclc.org/dsdl/schematron"<#if params?has_content> xmlns:efx="http://eforms.ted.europa.eu/efx" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"</#if> queryBinding="xslt2">

    <title>${title}</title>

    <#-- Namespace declarations - hardcoded as per design decision -->
    <ns prefix="xs" uri="http://www.w3.org/2001/XMLSchema" />
    <ns prefix="sch" uri="http://purl.oclc.org/dsdl/schematron" />
    <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" />
    <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" />
    <ns prefix="ext" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2" />
    <ns prefix="efac" uri="http://data.europa.eu/p27/eforms-ubl-extension-aggregate-components/1" />
    <ns prefix="efext" uri="http://data.europa.eu/p27/eforms-ubl-extensions/1" />
    <ns prefix="efbc" uri="http://data.europa.eu/p27/eforms-ubl-extension-basic-components/1" />
    <ns prefix="can" uri="urn:oasis:names:specification:ubl:schema:xsd:ContractAwardNotice-2" />
    <ns prefix="cn" uri="urn:oasis:names:specification:ubl:schema:xsd:ContractNotice-2" />
    <ns prefix="pin" uri="urn:oasis:names:specification:ubl:schema:xsd:PriorInformationNotice-2" />
    <ns prefix="fn" uri="http://www.w3.org/2005/xpath-functions" />
<#if params?has_content>
    <ns prefix="efx" uri="http://eforms.ted.europa.eu/efx" />

    <#-- API endpoint parameters (schema-level <let> becomes <xsl:param> via SchXSLT, overridable at runtime) -->
<#list params as param>
    <let name="${param.name}" value="${param.value}" />
</#list>

    <xsl:function name="efx:call-api" as="xs:integer">
        <xsl:param name="endpoint-url" as="xs:string"/>
        <xsl:param name="function" as="xs:string"/>
        <xsl:param name="args" as="xs:string*"/>
        <xsl:variable name="base-url" select="concat(
            if (ends-with($endpoint-url, '/')) then $endpoint-url else concat($endpoint-url, '/'),
            $function)"/>
        <xsl:variable name="query-params" select="string-join(
            for $i in 1 to count($args)
            return concat('arg', $i, '=', encode-for-uri(string($args[$i]))),
            '&amp;')"/>
        <xsl:variable name="url" select="if ($query-params != '') then concat($base-url, '?', $query-params) else $base-url"/>
        <xsl:variable name="response" select="
            if ($endpoint-url = '' or not(unparsed-text-available($url)))
            then '-1'
            else unparsed-text($url)"/>
        <xsl:value-of select="if ($response castable as xs:integer) then xs:integer($response) else -1"/>
    </xsl:function>
</#if>

    <#-- Global variables from schema-level LET statements -->
<#list letElements as letElement>
    <let name="${letElement.name}" value="${letElement.value?xml?replace("&apos;", "'")}"/>
</#list>

    <#-- Phases for each notice type -->
<#list phases as phase>
    <phase id="${phase.id}">
    <#list phase.activePatterns as pattern>
        <active pattern="EFORMS-${pattern}" />
    </#list>
    </phase>
</#list>

    <#-- Includes for all pattern files -->
<#list includes as include>
    <include href="${include}"/>
</#list>
<#if diagnostics?has_content>

    <diagnostics>
    <#list diagnostics as diagnostic>
        <diagnostic id="${diagnostic.id}" see="${diagnostic.seeAttribute}">${diagnostic.xpath}</diagnostic>
    </#list>
    </diagnostics>
</#if>

</schema>
