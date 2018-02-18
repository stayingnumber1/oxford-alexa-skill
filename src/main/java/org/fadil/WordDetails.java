package org.fadil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.amazonaws.util.json.JSONObject;

/**
 * This is a container class to hold the extracted information from
 * the {@link JSONObject} response obtained from calling the Oxford web service.
 * 
 * @author Fadil
 * @version 1.0
 * @since 17/02/2018
 */
public class WordDetails {
	
	private Optional<String> lexicalCategory;
	private Optional<String> definition;
	private List<String> examples;
	
	public WordDetails(Optional<String> lexicalCategory, Optional<String> definition, List<String> examples) {
		this.lexicalCategory = lexicalCategory;
		this.definition = definition;
		this.examples = examples;
	}

	public Optional<String> getLexicalCategory() {
		return lexicalCategory;
	}

	public Optional<String> getDefinition() {
		return definition;
	}

	public List<String> getExamples() {
		if (examples == null) {
			examples = new ArrayList<>();
		}
		return examples;
	}

	@Override
	public String toString() {
		return "WordDetails [lexicalCategory=" + lexicalCategory + ", definition=" + definition + ", examples="
				+ examples + "]";
	}

}
