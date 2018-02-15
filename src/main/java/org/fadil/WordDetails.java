package org.fadil;

public class WordDetails {
	
	private final String definition;
	private final String example;
	
	public WordDetails(String definition, String example) {
		this.definition = definition;
		this.example = example;
	}

	public String getDefinition() {
		return definition;
	}

	public String getExample() {
		return example;
	}

}
