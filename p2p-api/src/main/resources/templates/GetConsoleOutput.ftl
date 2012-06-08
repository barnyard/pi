<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
			    <instanceId>${InstanceId}</instanceId>
      </${Action}>
   </soap:Body>
</soap:Envelope>