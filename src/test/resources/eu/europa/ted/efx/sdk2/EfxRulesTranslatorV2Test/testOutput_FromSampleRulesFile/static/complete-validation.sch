<?xml version="1.0" encoding="utf-8" ?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">

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

    <let name="sdkVersion" value="&quot;2.0.0&quot;"/>
    <let name="testValue" value="&quot;test&quot;"/>

    <phase id="eforms-1">
        <active pattern="EFORMS-validation-stage-1a" />
        <active pattern="EFORMS-validation-stage-1b-1" />
        <active pattern="EFORMS-validation-stage-2a" />
        <active pattern="EFORMS-validation-stage-3a" />
    </phase>
    <phase id="eforms-2">
        <active pattern="EFORMS-validation-stage-1a" />
        <active pattern="EFORMS-validation-stage-1b-2" />
        <active pattern="EFORMS-validation-stage-2a" />
        <active pattern="EFORMS-validation-stage-3a" />
    </phase>
    <phase id="eforms-3">
        <active pattern="EFORMS-validation-stage-1a" />
        <active pattern="EFORMS-validation-stage-1b-3" />
        <active pattern="EFORMS-validation-stage-2a" />
        <active pattern="EFORMS-validation-stage-3a" />
    </phase>
    <phase id="eforms-4">
        <active pattern="EFORMS-validation-stage-1a" />
        <active pattern="EFORMS-validation-stage-2a" />
        <active pattern="EFORMS-validation-stage-3a" />
    </phase>
    <phase id="eforms-5">
        <active pattern="EFORMS-validation-stage-1a" />
        <active pattern="EFORMS-validation-stage-2a" />
        <active pattern="EFORMS-validation-stage-3a" />
    </phase>
    <phase id="eforms-E1">
        <active pattern="EFORMS-validation-stage-1a" />
        <active pattern="EFORMS-validation-stage-2a" />
        <active pattern="EFORMS-validation-stage-3a" />
    </phase>
    <phase id="eforms-E2">
        <active pattern="EFORMS-validation-stage-1a" />
        <active pattern="EFORMS-validation-stage-2a" />
        <active pattern="EFORMS-validation-stage-3a" />
    </phase>
    <phase id="eforms-X01">
        <active pattern="EFORMS-validation-stage-1a" />
        <active pattern="EFORMS-validation-stage-2a" />
        <active pattern="EFORMS-validation-stage-3a" />
    </phase>

    <diagnostics>
        <diagnostic id="ND-Root_BT-00-Text" see="field:BT-00-Text">PathNode/TextField</diagnostic>
        <diagnostic id="ND-SubNode_BT-00-Text" see="field:BT-00-Text">../PathNode/TextField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Code" see="field:BT-00-Code">PathNode/CodeField</diagnostic>
        <diagnostic id="ND-SubNode_BT-00-Indicator" see="field:BT-00-Indicator">../PathNode/IndicatorField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Indicator" see="field:BT-00-Indicator">PathNode/IndicatorField</diagnostic>
        <diagnostic id="ND-SubSubNode_BT-00-Indicator" see="field:BT-00-Indicator">../../PathNode/IndicatorField</diagnostic>
    </diagnostics>

    <include href="validation-stage-1a.sch"/>
    <include href="validation-stage-1b-1.sch"/>
    <include href="validation-stage-1b-2.sch"/>
    <include href="validation-stage-1b-3.sch"/>
    <include href="validation-stage-2a.sch"/>
    <include href="validation-stage-3a.sch"/>

</schema>
