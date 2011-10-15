package com.google.code.memoryfilesystem;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class StringTransformersTest {

	@Test
	public void identiy() {
		StringTransformer transformer = StringTransformers.IDENTIY;
		assertEquals("aA", transformer.tranform("aA"));
	}
	
	@Test
	public void caseInsensitive() {
		StringTransformer transformer = StringTransformers.caseInsensitive(Locale.US);
		assertEquals("aa", transformer.tranform("aA"));
	}

}
