package com.techmorphosis.grassroot.models;

import java.io.Serializable;

public class Create_GroupModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String name;

	private String emailId;

	private boolean isSelected;

	public Create_GroupModel() {

	}

	public Create_GroupModel(String name, String emailId) {

		this.name = name;
		this.emailId = emailId;

	}

	public Create_GroupModel(String name, String emailId, boolean isSelected) {

		this.name = name;
		this.emailId = emailId;
		this.isSelected = isSelected;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

}
