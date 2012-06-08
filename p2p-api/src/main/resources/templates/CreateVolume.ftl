<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
         <size>${Size}</size>
<#if SnapshotId??>
         <snapshotId>${SnapshotId}</snapshotId>
</#if>
         <availabilityZone>${AvailabilityZone}</availabilityZone>
      </${Action}>
   </soap:Body>
</soap:Envelope>