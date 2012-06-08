/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.entities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "error")
public class SimpleErrorMessageEntity {
	private String message;

	public SimpleErrorMessageEntity() {
	}

	public SimpleErrorMessageEntity(String aMessage) {
		this.message = aMessage;
	}

	@XmlElement
	public String getMessage() {
		return message;
	}
}
