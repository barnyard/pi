<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
         <groupName>${GroupName}</groupName>
         <groupDescription>${GroupDescription}</groupDescription>
      </${Action}>
   </soap:Body>
</soap:Envelope>