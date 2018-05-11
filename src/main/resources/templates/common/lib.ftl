<#macro message code type="">
<#if RequestParameters.msgid?default("") == 'true'>
${code}
<#else>
<#if type != 'SYMBOL'>${springMacroRequestContext.getMessage(code, code)}<#else><#assign messageText=springMacroRequestContext.getMessage(code + "_SYMBOL", "")><#if messageText == "">${springMacroRequestContext.getMessage(code, code)}<#else>${messageText}</#if></#if></#if></#macro>

<#macro messageArgs code args>
<#if RequestParameters.msgid?default("") == 'true'>
${code}
<#else>${springMacroRequestContext.getMessage(code, args, code)}</#if></#macro>

<#macro messageDefault text default="NO_LIST">
<#if text?has_content>${text}<#else>
<span class="fontGray"><@lib.message code=default /></span>
</#if>
</#macro>

<#-- header -->
<#macro header>
    <#include "/common/header.ftl">
</#macro>

<#-- top / 상단 영역 -->
<#macro top>
    <#include "/common/top.ftl">
</#macro>

<#-- sidebar / 왼쪽메뉴 영역-->
<#macro sidebar>
    <#include "/common/sidebar.ftl">
</#macro>

<#-- theme / 테마패널-->
<#macro theme>
    <#include "/common/theme.ftl">
</#macro>



<#-- footer -->
<#macro footer>
    <#include "/common/footer.ftl">
</#macro>


