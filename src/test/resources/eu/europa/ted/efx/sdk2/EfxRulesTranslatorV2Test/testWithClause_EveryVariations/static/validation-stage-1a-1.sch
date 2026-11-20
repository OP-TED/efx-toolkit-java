<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/RepeatableNode/FieldsPrivacy[exists(ReasonCode)]">
        <assert id="R-EVR-002" role="ERROR" diagnostics="ND-PrivacyInRepeatableNode_BT-198_BT-00_-Text-In-Repeatable-Node" test="exists(PublicationDate)">rule|text|R-EVR-002</assert>
    </rule>
</pattern>
