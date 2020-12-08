/*
 *  Copyright 2020 EPAM Systems
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.epam.reportportal.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.*;

public class MemoizingSupplierTest {

	@Mock
	private Supplier<String> supplier;

	@Test
	public void test_memoizing_supplier_calls_get_on_underline_supplier() {
		MemoizingSupplier<String> ms = new MemoizingSupplier<>(supplier);
		String expectedResult = RandomStringUtils.randomAlphabetic(10);
		when(supplier.get()).thenReturn(expectedResult);
		String actualResult = ms.get();

		verify(supplier).get();

		assertThat(actualResult, sameInstance(expectedResult));
	}

	@Test
	public void test_memoizing_supplier_returns_initialized_true_if_get_was_called() {
		MemoizingSupplier<String> ms = new MemoizingSupplier<>(supplier);
		ms.get();

		assertThat(ms.isInitialized(), equalTo(true));
	}

	@Test
	public void test_memoizing_supplier_returns_initialized_false_if_get_was_not_called() {
		MemoizingSupplier<String> ms = new MemoizingSupplier<>(supplier);
		assertThat(ms.isInitialized(), equalTo(false));
	}

	@Test
	public void test_memoizing_supplier_calls_get_on_underline_supplier_only_once() {
		MemoizingSupplier<String> ms = new MemoizingSupplier<>(supplier);
		String expectedResult = RandomStringUtils.randomAlphabetic(10);
		when(supplier.get()).thenReturn(expectedResult);
		ms.get();
		ms.get();

		verify(supplier).get();
	}

	@Test
	public void test_memoizing_supplier_calls_get_on_underline_supplier_if_it_was_reset() {
		MemoizingSupplier<String> ms = new MemoizingSupplier<>(supplier);
		String expectedResult1 = RandomStringUtils.randomAlphabetic(10);
		String expectedResult2 = RandomStringUtils.randomAlphabetic(10);
		when(supplier.get()).thenReturn(expectedResult1, expectedResult2);
		String actualResult1 = ms.get();
		ms.reset();
		String actualResult2 = ms.get();

		verify(supplier, times(2)).get();

		assertThat(actualResult1, sameInstance(expectedResult1));
		assertThat(actualResult2, sameInstance(expectedResult2));
	}
}
