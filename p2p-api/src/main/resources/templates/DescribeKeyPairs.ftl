<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
<#if KeyName??>
         <keySet>
   <#list KeyName as item>
      	    <item>
         	   <keyName>${item}</keyName>
            </item>
   </#list>
         </keySet>
</#if>
      </${Action}>
   </soap:Body>
</soap:Envelope>