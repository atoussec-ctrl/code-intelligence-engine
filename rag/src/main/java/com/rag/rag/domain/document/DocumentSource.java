package com.rag.rag.domain.document;

public record DocumentSource(DocumentSourceType type, String uri) {

	public DocumentSource {
		type = DomainValidation.required(type, "source type");
		uri = normalize(uri);
		if (type == DocumentSourceType.TEXT && uri != null) {
			throw new IllegalArgumentException("source uri is not allowed for text sources");
		}
		if (type != DocumentSourceType.TEXT) {
			uri = DomainValidation.requiredText(uri, "source uri");
		}
	}

	public static DocumentSource text() {
		return new DocumentSource(DocumentSourceType.TEXT, null);
	}

	public static DocumentSource url(String uri) {
		return new DocumentSource(DocumentSourceType.URL, uri);
	}

	public static DocumentSource github(String uri) {
		return new DocumentSource(DocumentSourceType.GITHUB, uri);
	}

	public static DocumentSource file(String uri) {
		return new DocumentSource(DocumentSourceType.FILE, uri);
	}

	private static String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
