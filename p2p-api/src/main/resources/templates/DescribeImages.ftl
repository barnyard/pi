<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
<#if ExecutableBy??>
         <executableBySet>
   <#list ExecutableBy as item>
      	    <item>
         	   <user>${item}</user>
            </item>
   </#list>
         </executableBySet>
</#if>
<#if Owner??>
         <ownersSet>
   <#list Owner as item>
      	    <item>
         	   <owner>${item}</owner>
            </item>
   </#list>
         </ownersSet>
</#if>
<#if ImageId??>
         <imagesSet>
   <#list ImageId as item>
      	    <item>
         	   <imageId>${item}</imageId>
            </item>
   </#list>
         </imagesSet>
</#if>
      </${Action}>
   </soap:Body>
</soap:Envelope>