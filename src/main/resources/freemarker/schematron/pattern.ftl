<?xml version="1.0" encoding="UTF-8"?>
<#--
  Template for individual Schematron pattern file.

  Parameters:
    id            - Pattern identifier (e.g., "validation-stage-1a-1")
    variables     - List<SchematronLet> of pattern-level variables
    rules         - List<SchematronRule> containing the validation rules
    tags          - List<String> of tags specifying which tests to include
-->
<pattern id="EFORMS-${id}" xmlns="http://purl.oclc.org/dsdl/schematron">
<#list variables as variable>
    <let name="${variable.name}" value="${variable.value?xml?replace("&apos;", "'")}"/>
</#list>
<#list rules as rule>
<#if rule.hasTestsForTags(tags)>
    <rule context="${rule.context}">
    <#list rule.variables as variable>
        <let name="${variable.name}" value="${variable.value?xml?replace("&apos;", "'")}"/>
    </#list>
    <#list rule.tests as test>
    <#if tags?seq_contains(test.tag)>
        <${test.elementName} id="${test.id}" role="${test.role}"<#if test.flag??> flag="${test.flag}"</#if><#if test.diagnostic??> diagnostics="${test.diagnostic.id}"</#if> test="${test.test?xml?replace("&apos;", "'")}">${test.message}</${test.elementName}>
    </#if>
    </#list>
    </rule>
</#if>
</#list>
</pattern>
