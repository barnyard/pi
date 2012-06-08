<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
<#if PublicIp??>
         <publicIpsSet>
   <#list PublicIp as item>
      	    <item>
         	   <publicIp>${item}</publicIp>
            </item>
   </#list>
         </publicIpsSet>
</#if>
      </${Action}>
   </soap:Body>
</soap:Envelope>