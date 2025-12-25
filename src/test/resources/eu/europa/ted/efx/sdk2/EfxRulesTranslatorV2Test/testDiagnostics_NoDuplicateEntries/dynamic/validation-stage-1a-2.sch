<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-K7P-M2Q" role="error" test=".">rule|text|R-K7P-M2Q</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <assert id="R-D4K-P9M" role="error" test="(not(./normalize-space(text()) != '')) or (true())">rule|text|R-D4K-P9M</assert>
    </rule>
</pattern>
