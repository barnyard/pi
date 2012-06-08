<html>
<head>
<title>
All of the users
</title>
</head>
<body>
	<#assign row=0>
	<#list users as thisuser>
		<#if (row % 2) == 0>
			<#assign bgcolor="lightblue">
		<#else>
			<#assign bgcolor="cream">
		</#if>
		<#assign row=row + 1>
		<table style="background-color: ${bgcolor};">
			<tr><td>Username:</td><td>${thisuser.username}</td></tr>
			<tr><td>Real name:</td><td>${thisuser.realName!''}</td></tr>
			<tr><td>Email address:</td><td>${thisuser.emailAddress!''}</td></tr>
			<tr><td>Api access key:</td><td>${thisuser.apiAccessKey!''}</td></tr>
			<tr><td>Api secret key:</td><td>${thisuser.apiSecretKey!''}</td></tr>
			<tr><td>Status:</td><td><#if thisuser.enabled>enabled<#else>disabled</#if></td></tr>
			<tr><td colspan="2">Download <a href="/users/${thisuser.username}/certificate">certificate</a></td></tr>
		</table>
	</#list>
<br />
<br />
Add a new user?
<form action="/users" method="post">
	<table>
		<tr><td>username</td><td><input type="text" name="username"/></td></tr>
		<tr><td>real name</td><td><input type="text" name="realname"/></td></tr>
		<tr><td>email</td><td><input type="text" name="email"/></td></tr>
		<tr><td>enabled</td><td><input type="checkbox" name="enabled" value="true" /></td></tr>
		<tr><td colspan="2"><input type="submit" value="add user" /></td></tr>
	</table>
</form>
</body>