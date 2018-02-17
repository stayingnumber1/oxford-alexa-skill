package org.fadil;

import java.util.List;
import java.util.Optional;

/**
 * This is a container class to hold the extracted information from
 * the {@link JSONObject} response obtained from calling the Oxford web service.
 * 
 * @author Fadil
 * @version 1.0
 * @since 17/02/2018
 */
public class WordDetails {
	
	private final Optional<String> lexicalCategory;
	private final Optional<String> definition;
	private final List<String> examples;
	
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
		return examples;
	}

	@Override
	public String toString() {
		return "WordDetails [lexicalCategory=" + lexicalCategory + ", definition=" + definition + ", examples="
				+ examples + "]";
	}

}
