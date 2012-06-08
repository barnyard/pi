<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
      	<regionSet>
      	 <#if Region??>
      	 <#list Region as item>
      	 <item>
         	<regionName>${item}</regionName>
         </item>
         </#list>
         </#if>
         </regionSet>
      </${Action}>
   </soap:Body>
</soap:Envelope>