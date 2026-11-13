package com.empresa.tickets.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.empresa.tickets.ai.dto.AnalisisTicketResponse;
import com.empresa.tickets.entity.enums.CategoriaTicket;
import com.empresa.tickets.entity.enums.PrioridadTicket;
import org.springframework.stereotype.Service;

@Service
public class HeuristicoAnalizadorService implements AnalizadorTicketService {

	private static final String ETIQUETA_ANALISIS_HEURISTICO = "analisis-heuristico";
	private static final int MAX_ETIQUETAS = 5;
	private static final int MAX_RESUMEN = 500;
	private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");

	@Override
	public AnalisisTicketResponse analizar(String titulo, String descripcion) {
		String texto = combinar(titulo, descripcion);
		String normalizado = normalizar(texto);
		CategoriaTicket categoria = clasificarCategoria(normalizado);
		PrioridadTicket prioridad = clasificarPrioridad(normalizado);

		Set<String> etiquetas = new LinkedHashSet<>();
		agregarEtiquetasCategoria(etiquetas, categoria);
		agregarEtiquetasPrioridad(etiquetas, normalizado, prioridad);
		etiquetas.add(ETIQUETA_ANALISIS_HEURISTICO);

		List<String> resultadoEtiquetas = new ArrayList<>(etiquetas);
		if (resultadoEtiquetas.size() > MAX_ETIQUETAS) {
			resultadoEtiquetas = new ArrayList<>(resultadoEtiquetas.subList(0, MAX_ETIQUETAS - 1));
			resultadoEtiquetas.add(ETIQUETA_ANALISIS_HEURISTICO);
		}
		return new AnalisisTicketResponse(categoria, prioridad, resumir(texto), resultadoEtiquetas);
	}

	private CategoriaTicket clasificarCategoria(String texto) {
		if (contieneAlguna(texto, "red", "wifi", "internet", "conectividad", "senal")) {
			return CategoriaTicket.CONECTIVIDAD;
		}
		if (contieneAlguna(texto, "acceso", "contrasena", "clave", "permiso", "bloqueado")) {
			return CategoriaTicket.ACCESOS;
		}
		if (contieneAlguna(texto, "pantalla", "teclado", "mouse", "impresora", "equipo", "computador")) {
			return CategoriaTicket.HARDWARE;
		}
		if (contieneAlguna(texto, "aplicacion", "software", "sistema", "programa", "error")) {
			return CategoriaTicket.SOFTWARE;
		}
		return CategoriaTicket.OTRO;
	}

	private PrioridadTicket clasificarPrioridad(String texto) {
		if (contieneAlguna(texto, "caido", "bloqueado")) {
			return PrioridadTicket.CRITICA;
		}
		if (contieneAlguna(texto, "urgente", "reunion")) {
			return PrioridadTicket.ALTA;
		}
		return PrioridadTicket.MEDIA;
	}

	private void agregarEtiquetasCategoria(Set<String> etiquetas, CategoriaTicket categoria) {
		switch (categoria) {
			case CONECTIVIDAD -> etiquetas.add("conectividad");
			case ACCESOS -> etiquetas.add("accesos");
			case HARDWARE -> etiquetas.add("hardware");
			case SOFTWARE -> etiquetas.add("software");
			case OTRO -> etiquetas.add("soporte");
		}
	}

	private void agregarEtiquetasPrioridad(Set<String> etiquetas, String texto, PrioridadTicket prioridad) {
		if (prioridad == PrioridadTicket.CRITICA) {
			etiquetas.add("critico");
		}
		else if (prioridad == PrioridadTicket.ALTA) {
			etiquetas.add("urgente");
		}
		if (contieneAlguna(texto, "reunion")) {
			etiquetas.add("reunion");
		}
		if (contieneAlguna(texto, "wifi", "internet", "red")) {
			etiquetas.add("red");
		}
	}

	private boolean contieneAlguna(String texto, String... palabras) {
		for (String palabra : palabras) {
			if (Pattern.compile("(?:^|[^a-z0-9])" + Pattern.quote(palabra) + "(?:$|[^a-z0-9])")
				.matcher(texto).find()) {
				return true;
			}
		}
		return false;
	}

	private String combinar(String titulo, String descripcion) {
		String tituloSeguro = titulo == null ? "" : titulo.trim();
		String descripcionSegura = descripcion == null ? "" : descripcion.trim();
		if (tituloSeguro.isEmpty()) {
			return descripcionSegura;
		}
		if (descripcionSegura.isEmpty()) {
			return tituloSeguro;
		}
		return tituloSeguro + ". " + descripcionSegura;
	}

	private String resumir(String texto) {
		if (texto.isBlank()) {
			return "Solicitud de soporte sin descripción suficiente.";
		}
		if (texto.length() <= MAX_RESUMEN) {
			return texto;
		}
		return texto.substring(0, MAX_RESUMEN - 1).stripTrailing() + "…";
	}

	private String normalizar(String texto) {
		String descompuesto = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		return ACENTOS.matcher(descompuesto).replaceAll("");
	}
}
