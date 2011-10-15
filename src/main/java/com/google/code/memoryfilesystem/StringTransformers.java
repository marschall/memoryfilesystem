package com.google.code.memoryfilesystem;

import java.util.Locale;

public final class StringTransformers {
	
	public static final StringTransformer IDENTIY = new IdentityTransformer();
	
	public StringTransformer caseInsensitive() {
		return caseInsensitive(Locale.getDefault());
	}
	
	public  static StringTransformer caseInsensitive(Locale locale) {
		return new CaseInsenstive(locale);
	}
	
	static final class CaseInsenstive implements StringTransformer {
		
		private final Locale locale;
		
		CaseInsenstive(Locale locale) {
			this.locale = locale;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String tranform(String s) {
			return s.toLowerCase(this.locale);
		}
		
	}
	static final class IdentityTransformer implements StringTransformer {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String tranform(String s) {
			return s;
		}
		
	}
	
}
