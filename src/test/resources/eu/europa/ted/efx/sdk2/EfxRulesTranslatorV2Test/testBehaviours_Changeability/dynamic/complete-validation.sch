<?xml version="1.0" encoding="utf-8" ?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:efx="http://eforms.ted.europa.eu/efx" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" queryBinding="xslt3">

    <title>eForms schematron rules</title>

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
    <ns prefix="efx" uri="http://eforms.ted.europa.eu/efx" />

    <let name="apiUrl-default" value="'https://api.ted.europa.eu/v1'" />

    <xsl:function name="efx:call-api" as="xs:integer">
        <xsl:param name="endpoint-name" as="xs:string"/>
        <xsl:param name="function" as="xs:string"/>
        <xsl:param name="query" as="xs:string"/>
        <xsl:variable name="endpoint-url" as="xs:string">
            <xsl:choose>
                <xsl:when test="$endpoint-name = 'default'"><xsl:value-of select="$apiUrl-default"/></xsl:when>
                <xsl:otherwise><xsl:value-of select="''"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="base-url" select="concat(
            if (ends-with($endpoint-url, '/')) then $endpoint-url else concat($endpoint-url, '/'),
            $function)"/>
        <xsl:variable name="url" select="if ($query != '') then concat($base-url, '?', $query) else $base-url"/>
        <xsl:variable name="response" as="xs:string">
            <xsl:try select="if ($endpoint-url = '') then '-1' else unparsed-text($url)">
                <xsl:catch select="'-1'"/>
            </xsl:try>
        </xsl:variable>
        <xsl:value-of select="if ($response castable as xs:integer) then xs:integer($response) else -1"/>
    </xsl:function>


    <phase id="eforms-1">
        <active pattern="EFORMS-validation-stage-6a-1" />
    </phase>

    <include href="validation-stage-6a-1.sch"/>

    <diagnostics>
        <diagnostic id="BT-00-Identifier_BT-00-Text" see="field:BT-00-Text">../TextField</diagnostic>
        <diagnostic id="BT-00-Identifier_BT-00-Code" see="field:BT-00-Code">../CodeField</diagnostic>
        <diagnostic id="BT-00-Identifier_BT-00-Repeatable-Text" see="field:BT-00-Repeatable-Text">../RepeatableTextField</diagnostic>
        <diagnostic id="BT-00-Identifier_BT-00-Repeatable-Number" see="field:BT-00-Repeatable-Number">../RepeatableNumberField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Text" see="field:BT-00-Text">PathNode/TextField</diagnostic>
    </diagnostics>

</schema>
