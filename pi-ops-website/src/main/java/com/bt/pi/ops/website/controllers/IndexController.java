package com.bt.pi.ops.website.controllers;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.sun.jersey.api.view.Viewable;

@Component
@Path("/index")
public class IndexController extends ControllerBase {
	public IndexController() {
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Viewable getAllHtml() {
		Map<String, Object> model = new HashMap<String, Object>();
		return new Viewable("index", model);
	}
}
