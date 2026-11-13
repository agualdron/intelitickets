package com.empresa.tickets.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.exception.AiAnalisisException;

public final class AnalisisTicketValidator {

	private static final int MAX_RESUMEN = 500;
	private static final int MAX_ETIQUETAS = 5;
	private static final int MIN_LONGITUD_ETIQUETA = 2;
	private static final int MAX_LONGITUD_ETIQUETA = 30;
	private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");
	private static final Pattern ESPACIOS_O_GUIONES_BAJOS = Pattern.compile("[\\s_]+");
	private static final Pattern CARACTERES_INVALIDOS = Pattern.compile("[^a-z0-9-]");
	private static final Pattern GUIONES_EXTERIORES = Pattern.compile("^-+|-+$");

	private AnalisisTicketValidator() {
	}

	public static AnalisisTicketResponse validar(AnalisisTicketResponse respuesta) {
		if (respuesta == null || respuesta.categoria() == null || respuesta.prioridad() == null
				|| respuesta.resumen() == null || respuesta.resumen().isBlank()
				|| respuesta.resumen().length() > MAX_RESUMEN || respuesta.etiquetas() == null) {
			throw new AiAnalisisException("El resultado de análisis no cumple el contrato esperado.");
		}

		LinkedHashSet<String> normalizadas = new LinkedHashSet<>();
		for (String etiqueta : respuesta.etiquetas()) {
			if (etiqueta == null) {
				continue;
			}
			String normalizada = normalizarEtiqueta(etiqueta);
			if (normalizada.length() < MIN_LONGITUD_ETIQUETA || normalizada.length() > MAX_LONGITUD_ETIQUETA) {
				continue;
			}
			normalizadas.add(normalizada);
		}

		List<String> etiquetasFinales = new ArrayList<>(normalizadas);
		if (etiquetasFinales.size() > MAX_ETIQUETAS) {
			etiquetasFinales = new ArrayList<>(etiquetasFinales.subList(0, MAX_ETIQUETAS));
		}
		if (etiquetasFinales.isEmpty()) {
			throw new AiAnalisisException("El resultado de análisis no contiene etiquetas válidas.");
		}

		return new AnalisisTicketResponse(respuesta.categoria(), respuesta.prioridad(), respuesta.resumen(),
			etiquetasFinales);
	}

	private static String normalizarEtiqueta(String etiqueta) {
		String minuscula = etiqueta.toLowerCase(Locale.ROOT);
		String descompuesta = Normalizer.normalize(minuscula, Normalizer.Form.NFD);
		String sinDiacriticos = MARCAS_DIACRITICAS.matcher(descompuesta).replaceAll("");
		String kebabCase = ESPACIOS_O_GUIONES_BAJOS.matcher(sinDiacriticos).replaceAll("-");
		String caracteresValidos = CARACTERES_INVALIDOS.matcher(kebabCase).replaceAll("");
		return GUIONES_EXTERIORES.matcher(caracteresValidos).replaceAll("");
	}
}
