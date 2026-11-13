package com.empresa.tickets.config;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortArgumentResolver;
import org.springframework.data.domain.Pageable;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

public class TicketPageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolver {

	private static final int MAX_PAGE_SIZE = 500;

	public TicketPageableHandlerMethodArgumentResolver(SortArgumentResolver sortResolver) {
		super(sortResolver);
	}

	@Override
	public Pageable resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		String size = webRequest.getParameter("size");
		if (size == null) {
			return super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
		}

		int requestedSize;
		try {
			requestedSize = Integer.parseInt(size);
		}
		catch (NumberFormatException exception) {
			throw new MethodArgumentTypeMismatchException(size, Integer.class, "size", methodParameter, exception);
		}

		if (requestedSize <= MAX_PAGE_SIZE) {
			return super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
		}

		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		HttpServletRequest normalizedRequest = new HttpServletRequestWrapper(servletRequest) {
			@Override
			public String getParameter(String name) {
				return "size".equals(name) ? Integer.toString(MAX_PAGE_SIZE) : super.getParameter(name);
			}

			@Override
			public String[] getParameterValues(String name) {
				return "size".equals(name) ? new String[] { Integer.toString(MAX_PAGE_SIZE) }
						: super.getParameterValues(name);
			}

			@Override
			public Map<String, String[]> getParameterMap() {
				Map<String, String[]> parameters = new LinkedHashMap<>(super.getParameterMap());
				parameters.put("size", new String[] { Integer.toString(MAX_PAGE_SIZE) });
				return parameters;
			}
		};
		NativeWebRequest normalizedWebRequest = new ServletWebRequest(normalizedRequest,
			webRequest.getNativeResponse(jakarta.servlet.http.HttpServletResponse.class));
		return super.resolveArgument(methodParameter, mavContainer, normalizedWebRequest, binderFactory);
	}
}
