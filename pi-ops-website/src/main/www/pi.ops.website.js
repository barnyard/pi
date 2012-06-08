optionsMap = {
		users : {
			title: 'Users'
		},
		instance : {
			title: 'Instance details',
			pickers: ['id'],
			url: '/dhtrecords/scopes/availability_zone/inst/{id}',
			display: 'table',
			fields: ['instanceId', 'state', 'type', 'launchTime', 'publicIpAddress', 'privateIpAddress', 'hostname']
		},
		userdetails: {
			title: 'Get User Details',
			pickers: ['username'],
			url: '/users/view/{username}',
			display: 'text'
		},
		userdisable: {
			title: 'Disable User',
			method: 'POST',
			pickers: ['username'],
			url: '/users/disable/{username}',
			display: 'text'
		},
		userenable: {
			title: 'Enable User',
			method: 'POST',
			pickers: ['username'],
			url: '/users/enable/{username}', 
			display: 'text'
		},
		userimages : {
			title: 'User images',
			pickers: ['username'],
			url: '/users/{username}/images.json',
			display: 'table',
			datapath: 'image',
			fields: ['imageId', 'manifestLocation', 'platform', 'architecture', 'public', 'state']					
		},
		useraddresses : {
			title: 'User addresses',
			pickers: ['username'],
			url: '/addresses/{username}',
			display: 'json'
		},
		usermax : {
			title: 'User max values',
			pickers: ['username', 'maxcores', 'maxinstances'],
			method: 'PUT',
			url: '/users/{username}',
			params: {
				maxInstances: '{maxinstances}',
				maxCores: '{maxcores}'
			},
			display: 'text'
		},
		dhtrecordglobal : {
			title: 'Global DHT record lookup',
			pickers: ['scheme', 'id'],
			pickerlabels: {
				scheme: 'URI scheme:',
				id: 'URI resource id:'
			},
			url: '/dhtrecords/global/{scheme}/{id}.json',
			display: 'json'
		},
		dhtrecordregion : {
			title: 'Region DHT record lookup',
			pickers: ['region', 'scheme', 'id'],
			pickerlabels: {
				scheme: 'URI scheme:',
				id: 'URI resource id:'
			},
			url: '/dhtrecords/regions/{region}/{scheme}/{id}.json',
			display: 'json'
		},
		dhtrecordavailabilityzone: {
			title: 'Availability zone DHT record lookup',
			pickers: ['availabilityzone', 'scheme', 'id'],
			pickerlabels: {
				scheme: 'URI scheme:',
				id: 'URI resource id:'
			},
			url: '/dhtrecords/availabilityzones/{availabilityzone}/{scheme}/{id}.json',
			display: 'json'
		},
		dhtrecordbyuri : {
			title: 'DHT record lookup by scope and URI',
			pickers: ['scope', 'scheme', 'id'],
			url: '/dhtrecords/scopes/{scope}/{scheme}/{id}.json',
			display: 'json'					
		},
		instancetypes : {
			title: 'Instance types',
			url: '/dhtrecords/instancetypes.json',
			display: 'json'
		},
		regions : {
			title: 'Regions',
			url: '/dhtrecords/regions.json',
			display: 'json'
		},
		availabilityzones : {
			title: 'Availability zones',
			url: '/dhtrecords/availabilityzones.json',
			display: 'json'
		},
		registerkernel : {
			title: 'Register a kernel',
			url: '/users/{username}/kernels',
			method: 'POST',
			display: 'table',
			fields: ['kernelId'],
			pickers: ['username', 'location'],
			pickerlabels: {
				username: 'Owner username:',
				location: 'Image manifest location:'
			},
			params: {
				image_manifest_location: '{location}'
			}
		},
		deregisterkernel : {
			title: 'Deregister a kernel',
			url: '/users/{username}/kernels/{id}',
			method: 'DELETE',
			display: 'table',
			fields: ['status'],
			pickers: ['username', 'id'],
			pickerlabels: {
			id: 'Kernel id:'
			}
		},
		registerramdisk : {
			title: 'Register a ramdisk',
			url: '/users/{username}/ramdisks',
			method: 'POST',
			display: 'table',
			fields: ['ramdiskId'],
			pickers: ['username', 'location'],
			pickerlabels: {
				username: 'Owner username:',
				location: 'Image manifest location:'
			},
			params: {
				image_manifest_location: '{location}'
			}
		},
		deregisterramdisk : {
			title: 'Deregister a ramdisk',
			url: '/users/{username}/ramdisks/{id}',
			method: 'DELETE',
			display: 'table',
			fields: ['status'],
			pickers: ['username', 'id'],
			pickerlabels: {
				id: 'Ramdisk id:'
			}
		},
		adduser : {
			title: 'Add a new user',
			url: '/users',
			method: 'POST',
			display: 'text',
			pickers: ['username', 'id', 'email', 'checkbox'],
			pickerlabels: {
				id: 'Real name:',
				checkbox: 'Enabled:'
			},
			params: {
				username: '{username}',
				realname: '{id}',
				enabled: '{checkbox}',
				email: '{email}',
				externalrefid: ''
			}
		},
		generatecert : {
			title: 'Generate new access cert / key for a user',
			url: '/users/{username}/pi-certs.zip',
			method: 'POST',
			display: 'text',
			pickers: ['username']
		},
		logerrors : {
			title: 'Show log file errors',
			pickers: ['availabilityzone', 'refresh'],
			url: '/availabilityzones/{availabilityzone}/alerts.json',
			display: 'table',
			datapath: 'entities',
			fields: ['timestamp', 'logMessage', 'nodeId', 'className', 'logTxId']
		},
		heartbeats : {
			title: 'Show nodes health status',
			pickers: ['availabilityzone', 'refresh'],
			pickerlabels: {
				availabilityzone: 'Availability Zone:'
			},
			url: '/availabilityzones/{availabilityzone}/heartbeats.json',
			display: 'table',
			datapath: 'entities',
			fields: ['timestamp', 'hostname', 'diskSpace', 'memoryDetails', 'leafSet', 'availableResources']
		},
		runninginstances : {
			title: 'Show all running instances',
			pickers: ['availabilityzone', 'refresh'],
			pickerlabels: {
				availabilityzone: 'Availability Zone:'
			},
			url: '/availabilityzones/{availabilityzone}/instances/running.json',
			display: 'table',
			datapath: 'entities',
			fields: ['instanceId', 'ownerId', 'launchTime', 'publicIpAddress', 'privateIpAddress']
		},
		zombieinstances : {
			title: 'Show all zombie instances',
			pickers: ['availabilityzone', 'refresh'],
			pickerlabels: {
				availabilityzone: 'Availability Zone:'
			},
			url: '/availabilityzones/{availabilityzone}/instances/zombie.json',
			display: 'table',
			datapath: 'entities',
			fields: ['instanceId', 'ownerId', 'launchTime']
		},
		deletezombieinstances : {
				title: 'Delete Zombie Instance',
				pickers: ['availabilityzone', 'id'],
				pickerlabels: {
					availabilityzone: 'Availability Zone:',
					id: 'Instance id:'
				},
				url: '/availabilityzones/{availabilityzone}/instances/terminate/{id}',
				method: 'DELETE',
				display: 'table',
				fields: ['instanceId', 'oldState', 'newState']
		},
		listapplications : {
				title: 'List Applications',			
				url: '/applications/list',
				method: 'GET',
				display: 'table',
				fields: ['nodeScope','value','applicationName', 'activeNodeMap']
		},
		deactivateapplication : {
				title: 'Deactivate Application on Node',
				pickers: ['applicationname', 'nodeid'],
				pickerlabels: {
					applicationname: 'Application Name:',
					nodeid: 'Node id:'
			    },
				url: '/applications/deactivate/{applicationname}/{nodeid}',
				method: 'GET',
				display: 'text'
		},changeimageplatform : {
			title: 'Change image platform',
			url: '/images/{id}/platform',
			method: 'POST',
			display: 'text',
			fields: ['id'],
			pickers: ['id', 'platform'],
			pickerlabels: {
				id: 'Image Id:',
				platform: 'Platform:'
			},
			params: {
				image_platform: '{platform}'
			}
		},
		availableresources : {
			title: 'Show available resources',
			pickers: ['availabilityzone'],
			pickerlabels: {
				availabilityzone: 'Availability Zone:'
		    },
			url: '/resources/{availabilityzone}',
			method: 'GET',
			display: 'table',
			fields: ['freeMemoryInMB', 'freeDiskInMB', 'freeCores','availableInstancesByType']
			
		},configuresupernodes : {
			title: 'Configure Supernodes',
			url: '/supernodes',
			method: 'POST',
			display: 'text',
			fields: ['id'],
			pickers: ['superNodeApplication', 'number', 'offset'],
			pickerlabels: {
				superNodeApplication: 'Supernode Application Name:',
				number: 'Number of supernodes as a power of 2:',
				offset: 'Offset from 0 that marks the 1st supernode Id:'
			},
			params: {
				superNodeApplication: '{superNodeApplication}',
				number: '{number}',
				offset: '{offset}'
			}
		},createmanagementuser : {
			title: 'Create Management User',
			url: '/managementusers',
			method: 'POST',
			display: 'text',
			pickers: ['username', 'password', 'role'],
			params: {
				username: '{username}',
				password: '{password}',
				roles: '{role}'
			}
		},findmanagementuser : {
			title: 'Find Management User',
			url: '/managementusers/{username}',
			method: 'GET',
			display: 'table',
			pickers: ['username'],
			fields: ['username', 'roles']
		},updatemanagementuser : {
			title: 'Update Management User',
			url: '/managementusers/{username}',
			method: 'PUT',
			display: 'text',
			pickers: ['username', 'password', 'role'],
			params: {
				username: '{username}',
				password: '{password}',
				roles: '{role}'
			}
		},deletemanagementuser : {
			title: 'Delete Management User',
			url: '/managementusers/{username}',
			method: 'DELETE',
			display: 'text',
			pickers: ['username']
		},allmanagementusers : {
			title: 'List all Management Users',
			url: '/managementusers',
			method: 'GET',
			display: 'table',
			datapath: 'managementUsers',
			fields: ['username', 'roles']
		},pauseinstance : {
			title: 'Pause Instance',
			url: '/instances/{id}/pause',
			pickerlabels: {
				id: 'Instance ID:'
			},
			method: 'POST',
			pickers: ['id'],
			display: 'text'
		}, unpauseinstance : {
			title: 'UnPause Instance',
			url: '/instances/{id}/unpause',
			pickerlabels: {
				id: 'Instance ID:'
			},
			pickers: ['id'],
			method: 'POST',
			display: 'text'
		},pauseinstanceipaddress : {
			title: 'Pause Instance By Ip Address',
			url: '/instances/pause',
			pickerlabels: {
				ipAddress: 'IP Address:'
			},
			method: 'POST',
			pickers: ['ipAddress'],
			display: 'text',
			params: {
				ipAddress: '{ipAddress}'
			}
		},
		unpauseinstanceipaddress : {
			title: 'Unpause Instance By Ip Address',
			url: '/instances/unpause',
			pickerlabels: {
				ipAddress: 'IP Address:'
			},
			method: 'POST',
			pickers: ['ipAddress'],
			display: 'text',
			params: {
				ipAddress: '{ipAddress}'
			}
		},
		getinstancevalidationpage : {
			title: 'Get user instance validation address',
			url: '/users/{username}/instancevalidation',
			method: 'GET',
			pickers: ['username'],
			display: 'text'
		},
		sendinstancevalidationemail : {
			title: 'Send instance validation email',
			url: '/users/{username}/instancevalidation',
			method: 'POST',
			pickers: ['username'],
			display: 'text'
		}
};
currentOption = undefined;
timer = undefined;

$(document).ready(function() {
	$.ajaxSetup({
		 cache: false,
		 timeout: 30000
	});

	$('.opsonly').hide();
	// get current user and show ops options if appropriate
	doAjax({
		url: '/currentuser.json',
		success : function(data) {
			if (data.username)
				$('#loggedInAs').text(data.username);
				
			if (data.roles && data.roles.toLowerCase().indexOf('_ops') > -1)
				$('.opsonly').show();
		},
		error : function(message) {
			$('#content #result').append($('<p style="color: red;">Error getting info on current user!<br/>' + message + '</p>'));
		}
	});
	
	// populate regions and avail zones
	doAjax({
		url: '/dhtrecords/regions.json',
		success : function(data) {
			$('#regionPicker select').children().remove().end();
			for (var key in data.regions) {
				if (key == 'toJSONString')
					continue;
				$('<option value="' + key + '">' + key + ' (' + data.regions[key].regionCode + ')</option>')
					.appendTo($('#regionPicker select'));
			};
		},
		error : function(message) {
			$('#content #result').append($('<p style="color: red;">Error getting regions!<br/>' + message + '</p>'));
		}
	});
	doAjax({
		url: '/dhtrecords/availabilityzones.json',
		success : function(data) {			
			$('#availabilityzonePicker select').children().remove().end();
			for (var key in data.availabilityZones) {
				if (key == 'toJSONString')
					continue;
				$('<option value="' + key + '">' + key + ' (' + data.availabilityZones[key].availabilityZoneCode + ')</option>')
					.appendTo($('#availabilityzonePicker select'));
			};
		},
		error : function(message) {
			$('#content #result').append($('<p style="color: red;">Error getting availability zones!<br/>' + message + '</p>'));
		}
	});
	
	doAjax({
		url: '/managementroles',
		success: function(data) {
			$('#rolePicker #roles').children().remove().end();
			for(var key in data) {
				if (key == 'toJSONString') 
					continue;
				
				$('<input type="checkbox" id="role" value="' + data[key] + '" name="' +  data[key] + '">' + data[key] + '</input><br/>').appendTo($('#rolePicker #roles'));
			}			
		},
		error: function(message) {
			$('#content #result').append($('<p style="color: red;">Error getting management roles!<br/>' + message + '</p>'));
		}
	});
	
	doAjax({
		url: '/environment/rpm',
		success: function(data) {
			$('#rpmdetails').append(data);
		},
		error: function(message) {
			$('#content #result').append($('<p style="color: red;">Error getting RPM details!<br/>' + message + '</p>'));
		}
	});
	
	$('#mainmenu > li > a').click(function(ev) {
		ev.preventDefault();
		$(ev.target).siblings('ul').toggle();				
	});

	$('.submenu li a').click(function(ev) {
		ev.preventDefault();
		currentOption = $(ev.target).attr('href').substring(1);
		displayCurrentOption();
	});
	
	$('#execute').click(function(ev) {
		ev.preventDefault();
		onExecute();
	});
	
	$('.submenu').hide();
});

function getCheckedBoxes(this_item) {
	var val = '';
	
	if ($('div#pickers input#' + this_item + ':checked').length > 0) {
		checked = '';
		$('div#pickers input#' + this_item + ':checked').each(function() {
			checked = checked + ';' + $(this).attr('name')
		});
			
		if (checked.substring(0, 1) == ';') { 
			  val = checked.substring(1);
		}
	}
	
	return val;
}

function onExecute() {
		
		$('#content').find('#result').remove().end().append($('<div id="result"><p><b>Result:</b></p></div>'));
	
		var conf = optionsMap[currentOption];
		
		if (conf.url) {
			var url = conf.url;
			
			// sort method
			var method = 'GET';
			if (conf.method)
				method = conf.method;
			
			var params = {};
			for (var key in conf.params) {
				params[key] = conf.params[key];
			}
			
			var refresh = false;
			
			// put picker values in url and params if specificed
			if (conf.pickers && conf.pickers.length > 0) {
				$.each(conf.pickers, function() {
					var picker = $('#content #' + this + 'Picker #' + this);
					var val = picker.is('input:checkbox') ? getCheckedBoxes(this) : picker.val();
					val = $.trim(val);									
					url = url.replace('{' + this + '}', val);
					if (this == 'refresh')
						refresh = picker.attr('checked');
					
					if (params) {
						for (var key in params) {
							if (key == 'toJSONString')
								continue;
											
							params[key] = params[key].replace('{' + this + '}', val);
						}
					}
				});
			}
			
			// create form params
			var data = '';
			if (params) {
				for (var key in params) {
					if (key == 'toJSONString')
						continue;
					data = data + '&' + key + '=' + params[key];
				}
			}
			if (data.length > 0)
				data = data.substring(1);
			
			// set data type
			var dataType = 'json';
			if (conf.display == 'text')
				dataType = 'text';
			
			// sort where data lives in the response
			var datapath = undefined;
			if (conf.datapath)
				datapath = conf.datapath;
								
			$('#content #result').append($('<p>Sending request to ' + url + '...</p>'));
			
			if (currentOption == 'generatecert') {
				// generating certs is a special case as we want to submit the form 
				$('#certCreator form').attr('action', url).attr('method', method).submit();
			} else {
				doAjax({
					url: url,
					type: method,
					data: data,
					dataType: dataType,
					success : function(data) {
						var content;
						if (conf.display == 'table') {
							content = renderObjectAsTable(data, datapath, conf['fields']);
						} else if (conf.display == 'json') {
							content = renderObjectAsJson(data);
						} else if (conf.display == 'text') {
							content = 'Got success response\n\n';
							content += data;
						} else {
							content = "!! Unknown display type: " + conf.display + " !!";
						}
						
						$('#content #result').append(content);
					},
					error : function(message) {
						$('#content #result').append($('<p style="color: red;">:( Error ):<br/>' + message + '</p>'));
					}
				});
			}			
			
			if (refresh) {
				$.timer(120000, function (timer) {
			    	onExecute();
			    	timer.stop();
				});
			}
		}
};

function displayCurrentOption(optionId) {	
	var conf = optionsMap[currentOption];	
	if (!conf)
		return;
	
	$('#content #pickers > *, #content #result').remove();
	$('#content h2').text(conf.title);
	$('#content #execute').show();
	
	// add pickers for selection options
	if (conf.pickers) {
		$.each(conf.pickers, function() {
			var label = $('#' + this + 'Picker label').text();
			if (conf.pickerlabels && conf.pickerlabels[this]) {
				label = conf.pickerlabels[this];
			}
			$('#content #pickers').append(
				$('#' + this + 'Picker').clone().removeClass('hidden')
					.find('label').text(label).end()
			);
		});
	}
};

function doAjax(opts) {
	$.ajax({
		url: opts.url,
		type: opts.type,
		data: opts.data,
		dataType: opts.dataType,
		success: function(data) {
			if (opts && opts.success)
				opts.success(data);
		},
		error: function(xhr, status, error){
			if (opts && opts.error) {
				opts.error((xhr && xhr.messageText ? 'xhr: ' + xhr.messageText + ' ' : ''     )
						+ ' status: ' + status
						+ (error ? ' error: ' + error : ''));
			}
		}
	});
};

function renderObjectAsTable(obj, datapath, fields) {
	var res = $('<table></table>');
	
	// row headings
	res.append('<tr></tr>');
	$.each(fields, function() {
		res.find('tr:last').append('<th>' + this + '</th>');
	});
	
	// rows
	var data = datapath ? obj[datapath] : obj;
	if (!(data instanceof Array)) {
		data = [data];
	}
	$.each(data, function() {
		var currItem = this;
		res.append('<tr></tr>');
		$.each(fields, function() {
			res.find('tr:last').append('<td></td>');
			res.find('td:last').append(JSON.stringify(currItem[this]));				
		});
	});
	
	return res;
};

function renderObjectAsJson(obj) {	
	return $('<pre>' + (obj ? obj.toJSONString(true) : 'no data returned!') + '</pre>');
};
