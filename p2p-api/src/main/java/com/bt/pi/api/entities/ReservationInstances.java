/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.entities;

import java.util.HashSet;
import java.util.Set;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.Reservation;

public class ReservationInstances {
	private Reservation reservation;
	private Set<Instance> instances;
	
	public ReservationInstances(){
		instances = new HashSet<Instance>();
	}
	
	public ReservationInstances(Reservation aReservation, Set<Instance> anInstancesSet) {
		super();
		this.reservation = aReservation;
		this.instances = anInstancesSet;
	}
	
	public Set<Instance> getInstances() {
		return instances;
	}
	
	public Reservation getReservation() {
		return reservation;
	}
	
	public void setReservation(Reservation aRreservation) {
		this.reservation = aRreservation;
	}
	
	
}
