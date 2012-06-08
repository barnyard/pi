<html>
<head><title>User Instance Validation</title></head>
<body>
    <h1>User Instance Validation</h1>
    <form action="/users/${user.getUsername()}/instancevalidation/${pid}" method="post">
	    <table>
	    	<tr><th>Instance Id</th><th>Validate?</th></tr>
	    <#list instances as instance>
		    <#assign bgcolor="${instance.getInstanceActivityState()}">
		    <#if bgcolor="AMBER">
		        <#assign bgcolor="YELLOW"/>
		    </#if>
		    <tr style="background-color: ${bgcolor};">
			    <td>${instance.getInstanceId()}</td><td align="center"><input type="checkbox" name="validate_${instance.getInstanceId()}" value="true"/></td>
		    </tr>
	     </#list>
	     </table>
	     <input type="submit" value="validate selected instances" />
	 </form>

<br/>
<br/>

<table>
 <tr><th colspan="2"><small><small>Legend</small></small></th></tr>
 <tr><td style="background-color: RED;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td><small><small>Instance has been paused due to non validation and will soon be terminated</small></small></td></tr>
 <tr><td style="background-color: YELLOW;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td><small><small>Instance has been not be validated for over 28 days and will soon be paused</small></small></td></tr>
 <tr><td style="background-color: GREEN;">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td><small><small>Instance has been validated within the last 28 days</small></small></td></tr>
</table>

</body>
</html>
