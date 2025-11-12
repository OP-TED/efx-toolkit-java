<?xml version="1.0" encoding="utf-8" ?>
<#-- Template for complete-validation.sch master file -->
<schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">

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

    <#-- Global variables from schema-level LET statements -->
<#list globalVariables as variable>
    <let name="${variable.name}" value="${variable.value}"/>
</#list>

    <#-- Phases for each notice type -->
<#list phases as phase>
    <phase id="${phase.id}">
    <#list phase.activePatterns as pattern>
        <active pattern="${pattern}" />
    </#list>
    </phase>
</#list>

    <#-- Includes for all pattern files -->
<#list includes as include>
    <include href="${include}"/>
</#list>

</schema>
