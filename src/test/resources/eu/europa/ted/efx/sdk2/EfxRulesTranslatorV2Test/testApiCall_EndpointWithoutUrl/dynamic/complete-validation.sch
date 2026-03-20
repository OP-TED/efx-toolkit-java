<?xml version="1.0" encoding="utf-8" ?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:efx="http://eforms.ted.europa.eu/efx" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" queryBinding="xslt2">

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

    <let name="apiUrl-staging" value="''" />

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


    <phase id="eforms-1">
        <active pattern="EFORMS-validation-stage-1a-1" />
    </phase>

    <include href="validation-stage-1a-1.sch"/>

</schema>
