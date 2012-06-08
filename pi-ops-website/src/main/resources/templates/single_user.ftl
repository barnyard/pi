<html>
<head>
<title>
user ${user.username}
</title>
</head>
<body>
<table>
	<tr><td>Username:</td><td>${user.username}</td></tr>
	<tr><td>Real name:</td><td>${user.realName!''}</td></tr>
	<tr><td>Email address:</td><td>${user.emailAddress!''}</td></tr>
	<tr><td>Api access key:</td><td>${user.apiAccessKey!''}</td></tr>
	<tr><td>Api secret key:</td><td>${user.apiSecretKey!''}</td></tr>
	<tr><td>Status:</td><td><#if user.enabled>enabled<#else>disabled</#if></td></tr>
	<tr><td>Maximum cores:</td><td>${user.maxCores!''}</td></tr>
	<tr><td>Maximum instances:</td><td>${user.maxInstances!''}</td></tr>
	<tr><td colspan="2">Download <a href="/users/${user.username}/certificate">certificate</a></td></tr>
	<tr><td colspan="2">
		<#if (securityGroupIds?size >0) >
			Security Groups:
			<table>
				<#assign row=0>
				<#list securityGroupIds?sort as it>
					<#if (row % 2) == 0>
						<#assign bgcolor="lightblue">
					<#else>
						<#assign bgcolor="cream">
					</#if>
					<#assign row=row + 1>
					<tr style="background-color: ${bgcolor};"><td>${it}</td></tr>
				</#list>
			</table>
		<#else>
			(user has no security groups)
		</#if>	
	</td></tr>
	
	<tr><td colspan="2">
		<#if (instanceIds?size >0) >
			Instances:
			<table>
				<#assign row=0>
				<#list instanceIds?sort as it>
					<#if (row % 2) == 0>
						<#assign bgcolor="lightblue">
					<#else>
						<#assign bgcolor="cream">
					</#if>
					<#assign row=row + 1>
					<tr style="background-color: ${bgcolor};"><td>${it}</td></tr>
				</#list>
			</table>
		<#else>
			(user has no instances)
		</#if>	
	</td></tr>
	
	<tr><td colspan="2">
		<#if (volumeIds?size >0) >
			Volumes:
			<table>
				<#assign row=0>
				<#list volumeIds?sort as it>
					<#if (row % 2) == 0>
						<#assign bgcolor="lightblue">
					<#else>
						<#assign bgcolor="cream">
					</#if>
					<#assign row=row + 1>
					<tr style="background-color: ${bgcolor};"><td>${it}</td></tr>
				</#list>
			</table>
		<#else>
			(user has no volumess)
		</#if>	
	</td></tr>
	
	<tr><td colspan="2">
		<#if (imageIds?size >0) >
			Images:
			<table>
				<#assign row=0>
				<#list imageIds?sort as it>
					<#if (row % 2) == 0>
						<#assign bgcolor="lightblue">
					<#else>
						<#assign bgcolor="cream">
					</#if>
					<#assign row=row + 1>
					<tr style="background-color: ${bgcolor};"><td><a href="/users/${user.username}/images/${it}">${it}</a></td></tr>
				</#list>
			</table>
		<#else>
			(user has no images)
		</#if>	
	</td></tr>
</table>
</body>