package com.rag.rag.domain.document;

public record DocumentSource(DocumentSourceType type, String uri) {

	public DocumentSource {
		type = DomainValidation.required(type, "source type");
		uri = normalize(uri);
	}

	public static DocumentSource text() {
		return new DocumentSource(DocumentSourceType.TEXT, null);
	}

	public static DocumentSource url(String uri) {
		return new DocumentSource(DocumentSourceType.URL, DomainValidation.requiredText(uri, "source uri"));
	}

	private static String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}