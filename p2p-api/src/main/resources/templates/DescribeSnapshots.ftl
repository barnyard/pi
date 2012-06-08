<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
      	<snapshotSet>
      	 <#if SnapshotId??>
      	 <#list SnapshotId as item>
      	 <item>
         	<snapshotId>${item}</snapshotId>
         </item>
         </#list>
         </#if>
         </snapshotSet>
      </${Action}>
   </soap:Body>
</soap:Envelope>