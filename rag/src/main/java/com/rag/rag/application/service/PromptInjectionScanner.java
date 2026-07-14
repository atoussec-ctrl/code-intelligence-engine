package com.rag.rag.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class PromptInjectionScanner {

	public static final String INJECTION_SUSPECTED_KEY = "security.injection_suspected";
	public static final String INJECTION_REASONS_KEY = "security.injection_reasons";

	private static final List<Rule> RULES = List.of(
		new Rule("instruction_override", Pattern.compile("\\b(ignore|disregard|forget)\\s+(all\\s+)?(previous|prior|above)\\s+instructions?\\b")),
		new Rule("prompt_exfiltration", Pattern.compile("\\b(reveal|show|print|dump|leak)\\s+(the\\s+)?(system prompt|developer message|hidden instructions|internal policy)\\b|\\b(system prompt|developer message|hidden instructions|internal policy)\\b")),
		new Rule("secret_exfiltration", Pattern.compile("\\b(reveal|show|print|dump|leak|exfiltrate)\\s+(the\\s+)?(secrets?|credentials?|api keys?|tokens?)\\b|\\b(credentials?|api keys?|tokens?)\\b")),
		new Rule("tool_misuse", Pattern.compile("\\b(call|execute|invoke|use)\\s+(a\\s+)?(tool|function)\\b")),
		new Rule("citation_bypass", Pattern.compile("\\b(do not|don't|never)\\s+(cite|include citations?|mention sources?)\\b|\\bwithout\\s+citations?\\b"))
	);

	public Map<String, String> scan(String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content is required");
		}

		String normalized = content.toLowerCase(Locale.ROOT);
		List<String> reasons = new ArrayList<>();
		for (Rule rule : RULES) {
			if (rule.matches(normalized)) {
				reasons.add(rule.reason());
			}
		}

		Map<String, String> metadata = new LinkedHashMap<>();
		metadata.put(INJECTION_SUSPECTED_KEY, Boolean.toString(!reasons.isEmpty()));
		if (!reasons.isEmpty()) {
			metadata.put(INJECTION_REASONS_KEY, String.join(",", reasons));
		}
		return Map.copyOf(metadata);
	}

	private record Rule(String reason, Pattern pattern) {
		private boolean matches(String content) {
			return pattern.matcher(content).find();
		}
	}
}
