<?xml version="1.0" encoding="utf-8" ?>
<schema xmlns="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt3">

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


    <phase id="eforms-1">
        <active pattern="EFORMS-validation-stage-1a-1" />
    </phase>

    <include href="validation-stage-1a-1.sch"/>

    <diagnostics>
        <diagnostic id="ND-Root_BT-00-Text" see="field:BT-00-Text">PathNode/TextField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Number" see="field:BT-00-Number">PathNode/NumberField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Repeatable-Text" see="field:BT-00-Repeatable-Text">PathNode/RepeatableTextField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Integer" see="field:BT-00-Integer">PathNode/IntegerField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Code" see="field:BT-00-Code">PathNode/CodeField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Indicator" see="field:BT-00-Indicator">PathNode/IndicatorField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Identifier" see="field:BT-00-Identifier">PathNode/IdField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Repeatable-Number" see="field:BT-00-Repeatable-Number">PathNode/RepeatableNumberField</diagnostic>
        <diagnostic id="ND-Root_BT-00-Amount-Currency" see="field:BT-00-Amount-Currency">PathNode/AmountField/@currencyID</diagnostic>
        <diagnostic id="ND-Root_ND-RepeatableNode" see="node:ND-RepeatableNode">RepeatableNode</diagnostic>
        <diagnostic id="ND-Root_ND-SubNode" see="node:ND-SubNode">SubNode</diagnostic>
        <diagnostic id="ND-SubNode_BT-01-SubNode-Text" see="field:BT-01-SubNode-Text">SubTextField</diagnostic>
        <diagnostic id="ND-RepeatableNode_BT-00-Text-In-Repeatable-Node" see="field:BT-00-Text-In-Repeatable-Node">TextField</diagnostic>
    </diagnostics>

</schema>
