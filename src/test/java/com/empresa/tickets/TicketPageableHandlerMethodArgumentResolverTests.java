package com.empresa.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.empresa.tickets.config.TicketPageableHandlerMethodArgumentResolver;

class TicketPageableHandlerMethodArgumentResolverTests {

	@Test
	void clampsSizeBeforeResolvingPageable() throws Exception {
		Pageable pageable = resolve("2001");
		assertEquals(1000, pageable.getPageSize());
	}

	@Test
	void preservesValidPageSize() throws Exception {
		Pageable pageable = resolve("37");
		assertEquals(37, pageable.getPageSize());
	}

	@Test
	void rejectsNonIntegerPageSize() {
		assertThrows(MethodArgumentTypeMismatchException.class, () -> resolve("large"));
	}

	private Pageable resolve(String size) throws Exception {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.setParameter("size", size);
		var resolver = new TicketPageableHandlerMethodArgumentResolver(new SortHandlerMethodArgumentResolver());
		resolver.setMaxPageSize(1000);
		Method endpoint = getClass().getDeclaredMethod("endpoint", Pageable.class);
		return resolver.resolveArgument(new org.springframework.core.MethodParameter(endpoint, 0),
			new ModelAndViewContainer(), new ServletWebRequest(servletRequest), null);
	}

	@SuppressWarnings("unused")
	private void endpoint(Pageable pageable) {
	}
}
