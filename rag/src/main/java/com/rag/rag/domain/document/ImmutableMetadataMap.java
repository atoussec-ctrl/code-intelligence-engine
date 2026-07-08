package com.rag.rag.domain.document;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

final class ImmutableMetadataMap extends AbstractMap<String, String> {

	private final Map<String, String> delegate;
	private final String mutationMessage;

	private ImmutableMetadataMap(Map<String, String> delegate, String mutationMessage) {
		this.delegate = delegate;
		this.mutationMessage = mutationMessage;
	}

	static Map<String, String> from(Map<String, String> delegate, String mutationMessage) {
		return new ImmutableMetadataMap(delegate, mutationMessage);
	}

	@Override
	public Set<Entry<String, String>> entrySet() {
		return delegate.entrySet();
	}

	@Override
	public String put(String key, String value) {
		throw immutable();
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> map) {
		throw immutable();
	}

	@Override
	public String remove(Object key) {
		throw immutable();
	}

	@Override
	public void clear() {
		throw immutable();
	}

	private UnsupportedOperationException immutable() {
		return new UnsupportedOperationException(mutationMessage);
	}

}