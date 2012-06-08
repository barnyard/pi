<html>
<head>
	<title>The Pi Management Super Cool Tool</title>
	<link rel="stylesheet" type="text/css" href="default.css" />
</head>
<body>
	<script type="text/javascript" src="jquery-1.4.2.min.js"></script>
	<script type="text/javascript" src="json2.js"></script>
	<script type="text/javascript" src="JSON-js-prettyPrint.js"></script>
	<script type="text/javascript" src="jquery-timer.js"></script>
	<script type="text/javascript" src="pi.ops.website.js"></script>
	
	<div id="header" class="section">
		<p>Logged in as <b id="loggedInAs"></b> <a href="/logout">Log out</a></p> 
	</div>
	
	<div id="menu" class="section">
		<img src="pi-logo.png" height="50px" width="50px"/>
		<h2>Pi Management</h2>
		
		<ul id="mainmenu">
			<li>
				<a href="#">System status</a>
				<ul class="submenu">
					<li><a href="#heartbeats" title="Displays latest node heartbeat information">Show node statuses</a></li>
					<li><a href="#availableresources" title="Displays free and available resources in any given availability zone">Show available resources</a></li>
					<li><a href="#instance" title="Displays details about any given instance, including: state, launch time, public and private IP addresses, and the hostname of the node that hosts the instance.">Show instance details</a></li>
					<li><a href="#instancetypes" title="Returns a description of all the instance types defined in the cluster. The information displayed includes: name, number of cores, memory size, and disk size.">Show instance types</a></li>
					<li><a href="#regions" title="Displays basic information of all the regions defined in the cluster">Show regions</a></li>
					<li><a href="#availabilityzones" title="Displays basic information about the availability zones defined in the cluster">Show availability zones</a></li>
					<li><a href="#runninginstances" title="Display a list of all the running instances in any given availability zone">Show running instances</a></li>
					<li><a href="#zombieinstances" title="Shows a list of all the zombie instances in any given availability zone. A 'zombie' instance is a instance that is still running although it shows as terminated in pi.">Show zombie instances</a></li>
					<li><a href="#listapplications" title="Shows basic information about all the applications running in the entire cluster">List applications</a></li>
				</ul>
			</li>
			<li class="opsonly">
				<a href="#">System configuration</a>
				<ul class="submenu">
					<li><a href="#deactivateapplication" title="Disables an application on any node. The application will usually be picked up by another node">Deactivate application on node</a></li>
					<li><a href="#configuresupernodes" title="Configures a supernode application for the cluster, given the desired number of supernodes per availability zone, as well as the offset (used to prevent having more than one supernode application in one node)">Configure supernodes</a></li>
				</ul>
			</li>
			<li class="opsonly">
				<a href="#">Diagnostics</a>
				<ul class="submenu">
					<li><a href="#logerrors" title="displays a list of error log entries for any availability zone. The listing is sorted so that the most recent entries appear first">Show log file errors</a></li>
					<li><a href="#dhtrecordbyuri" title="Given a DHT record Id, and a scope and URI, it will return the corresponding full entity record.">Look up DHT record by scope + URI</a></li>
					<li><a href="#dhtrecordglobal"title="Given a global DHT record Id, it will return the corresponding full entity record">Look up global DHT record</a></li>
					<li><a href="#dhtrecordregion" title="Given a DHT record Id, and region, it will return the corresponding full entity record">Look up DHT record in a region</a></li>
					<li><a href="#dhtrecordavailabilityzone" title="Given a DHT record Id, and any availability zone, it will return the corresponding full entity record">Look up DHT record in an av. zone</a></li>
				</ul>
			</li>
			<li>
				<a href="#">User and image management</a>
				<ul class="submenu">
					<li class="opsonly" title="Creates a brand new PI user"><a href="#adduser">Add a user</a></li>
					<li class="opsonly" title="Retrieves details of a PI user"><a href="#userdetails">Get user details</a></li>
					<li class="opsonly" title="Generates a new certificate and set of keys for the user, and returns a zip file."><a href="#generatecert">Generate new cert / key for user</a></li>
					<li class="opsonly" title="Disables an user"><a href="#userdisable">Disable user</a></li>
					<li class="opsonly" title="Enables an user"><a href="#userenable">Enable user</a></li>
					<li class="opsonly" title="Updates Max Cores and Max Instances allowed for the user"><a href="#usermax">Change user max values</a></li>
					<li class="opsonly" title="Shows the list of images for any given user" ><a href="#userimages">Show user images</a></li>
					<li class="opsonly" title="Shows the list of elastic IP addresses for any given user"><a href="#useraddresses">Show user addresses</a></li>
					<li class="opsonly" title="Utility that registers a new kernel image in PI"><a href="#registerkernel">Register a kernel</a></li>
					<li class="opsonly" title="Utility that de-registers an existing kernel image"><a href="#deregisterkernel">Deregister a kernel</a></li>
					<li class="opsonly" title="Utility that registers a new ramdisk image in PI"><a href="#registerramdisk">Register a ramdisk</a></li>
					<li class="opsonly" title="Utility that de-registers an existing kernel image"><a href="#deregisterramdisk">Deregister a ramdisk</a></li>
					<li class="opsonly" title="Used to change the platform (OS) of any image. "><a href="#changeimageplatform">Change Image Platform</a></li>
				</ul>
			</li>
			<li class="opsonly">
				<a href="#">Management Users</a>
				<ul class="submenu">
					<li><a href="#allmanagementusers" title="Displays the list of all the management users with the corresponding assigned roles">List all Management Users</a></li>
					<li><a href="#createmanagementuser" title="Create a new management user">Create Management User</a></li>
					<li><a href="#findmanagementuser" title="Used to retrieve DHT information of an user given the name">Find Management User</a></li>
					<li><a href="#updatemanagementuser" title="Used to update the user record for a management user. It can be used to change password or assigned roles.">Update Management User</a></li>
					<li><a href="#deletemanagementuser" title="Deletes a management user">Delete Management User</a></li>
				</ul>
			</li>
			<li class="opsonly">
				<a href="#">User Instances Management</a>
				<ul class="submenu">
					<li><a href="#deletezombieinstances" title="Terminates a zombie instance">Terminate a zombie instance</a></li>
					<li><a href="#pauseinstance" title="Pause an instance">Pause Instance</a></li>
					<li><a href="#pauseinstanceipaddress" title="Pauses an instance by ip address">Pause Instance by ip address</a></li>
					<li><a href="#unpauseinstance" title="UnPause an instance">UnPause Instance</a></li>
					<li><a href="#unpauseinstanceipaddress" title="Unpauses an instance by ip address">Unpause Instance by ip address</a></li>
					<li><a href="#getinstancevalidationpage" title="gets the url to validate a users instances">Get URL to validate a users instances</a></li>
					<li><a href="#sendinstancevalidationemail" title="sends instance validation email to the selected user">Send instance validation email</a></li>
				</ul>
			</li>
			<li>
				<a href="#">External Systems</a>
				<ul class="submenu">
					<a href="http://nagios.cloud21cn.com" target="_blank" title="Links to Cloud 21CN Nagios website">Nagios</a>				
				</ul>
			</li>
		</ul>
		<div id="footer" class="section">
			<small><i>"Nature is a mutable cloud which is always and never the same."</i> - Ralph Waldo Emerson</small>
		</div>
	</div>
	
	<div id="content" class="section">
		<h2>Welcome</h2>
		<div id="pickers">
		</div>
		<button id="execute" type="button" class="button" style="display:none">Execute</button>
		<div id="result">This is the Pi Management tool. Pi RPM running on this environment : <div id="rpmdetails"></div></div>
	</div>
	
	<div id="usernamePicker" class="picker hidden">
		<label for="username">Username:</label>
		<input id="username" name="username" type="text"/>
	</div>
	
	<div id="emailPicker" class="picker hidden">
		<label for="email">Email address:</label>
		<input id="email" name="email" type="text"/>
	</div>
	
	<div id="maxcoresPicker" class="picker hidden">
		<label for="maxcores">Max Cores:</label>
		<input id="maxcores" name="maxcores" type="text" value="UNCHANGED"/>
		<small>leave blank to reset to default</small>
	</div>
	
	<div id="maxinstancesPicker" class="picker hidden">
		<label for="maxinstances">Max Instances:</label>
		<input id="maxinstances" name="maxinstances" type="text" value="UNCHANGED"/>
		<small>leave blank to reset to default</small>
	</div>
	
	<div id="idPicker" class="picker hidden">
		<label for="id">Id:</label>
		<input id="id" name="id" type="text"/>
	</div>
	
	<div id="locationPicker" class="picker hidden">
		<label for="location">Location:</label>
		<input id="location" name="location" type="text"/>
	</div>
	
	<div id="nodeidPicker" class="picker hidden">
		<label for="nodeid">Node ID:</label>
		<input id="nodeid" name="nodeid" type="text"/>
	</div>
	<div id="applicationnamePicker" class="picker hidden">
		<label for="applicationname">Application Name:</label>
		<input id="applicationname" name="applicationname" type="text"/>
	</div>
	
	<div id="checkboxPicker" class="picker hidden">
		<label for="checkbox">Enabled:</label>
		<input id="checkbox" name="checkbox" type="checkbox" checked="yes"/>
	</div>
	
	<div id="refreshPicker" class="picker hidden">
		<label for="checkbox">Auto refresh:</label>
		<input id="refresh" name="refresh" type="checkbox"/>
	</div>
	
	<div id="scopePicker" class="picker hidden">
		<label for="scope">Scope:</label>
		<select id="scope" name="scope">
			<option value="global">Global</option>
			<option value="region">Region</option>
			<option value="availability_zone">Availability zone</option>
		</select>
	</div>
	
	<div id="regionPicker" class="picker hidden">
		<label for="region">Region:</label>
		<select id="region" name="region">			
		</select>
	</div>
	
	<div id="availabilityzonePicker" class="picker hidden">
		<label for="availabilityzone">Availability zone:</label>
		<select id="availabilityzone" name="availabilityzone">			
		</select>
	</div>
	
	<div id="platformPicker" class="picker hidden">
		<label for="platform">Platform:</label>
		<select id="platform" name="platform">
			<option value="windows">Windows</option>
			<option value="linux">Linux</option>
		</select>
	</div>
	
	<div id="superNodeApplicationPicker" class="picker hidden">
		<label for="superNodeApplication">SuperNodeApplication:</label>
		<select id="superNodeApplication" name="superNodeApplication">
			<option value="pi-reporting-app">pi-reporting-app</option>
		</select>
	</div>
	
	<div id="numberPicker" class="picker hidden">
		<label for="number">Numbers:</label>
		<input id="number" name="number" type="text"/>
	</div>
	
	<div id="offsetPicker" class="picker hidden">
		<label for="offset">Offset:</label>
		<input id="offset" name="offset" type="text"/>
	</div>
	
	<div id="ipAddressPicker" class="picker hidden">
		<label for="ipAddress">Ip Address:</label>
		<input id="ipAddress" name="ipAddress" type="text"/>
	</div>
	
	<div id="passwordPicker" class="picker hidden">
		<label for="password">Password:</label>
		<input id="password" name="password" type="password"/>
	</div> 
	
	<div id="rolePicker" class="picker hidden">
		<label for="Role">Role:</label><br/>
		<div id="roles"></div>
	</div>
	
	<div id="schemePicker" class="picker hidden">
		<label for="scheme">Scheme:</label>
		<select id="scheme" name="scheme">
			<option value="inst">inst</option>
			<option value="user">user</option>
			<option value="avzapp">avzapp</option>
			<option value="regionapp">regionapp</option>
			<option value="bucketMetaData">bucketMetaData</option>
			<option value="idx">idx</option>
			<option value="sg">sg</option>
			<option value="topic">topic</option>
			<option value="vol">vol</option>
			<option value="queue">queue</option>
			<option value="avz">avz</option>
			<option value="rgn">rgn</option>
			<option value="pcrt">pcrt</option>
			<option value="addr">addr</option>
			<option value="vlan">vlan</option>
			<option value="img">img</option>
			<option value="uak">uak</option>
			<option value="snapp">snapp</option>
		</select>
	</div>
	
	<div id="certCreator" class="hidden">
		<form id="certCreatorForm" method="post" action="/users/{username}/certificate">
		</form>		
	</div>
</body>
</html>
