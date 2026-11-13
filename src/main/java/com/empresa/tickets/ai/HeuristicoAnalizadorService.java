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

	private CategoriaTicket clasificarCategoria(String textoOriginal) {
		String texto = normalizar(textoOriginal);

		// 1. CONECTIVIDAD (Redes, infraestructura, acceso remoto y telefonía IP)
		if (contieneAlguna(texto,
				"red", "wifi", "wi-fi", "internet", "conectividad", "senal", "conexion",
				"desconexion", "desconectado", "sin linea", "vpn", "ethernet", "cable de red",
				"router", "switch", "modem", "ip", "dns", "proxy", "navegar", "navegacion",
				"lento el internet", "latencia", "paquetes", "intranet", "red interna")) {
			return CategoriaTicket.CONECTIVIDAD;
		}

		// 2. ACCESOS (Identidad, credenciales, permisos, MFA y cuentas)
		if (contieneAlguna(texto,
				"acceso", "accesos", "contrasena", "clave", "password", "pin",
				"permiso", "permisos", "autorizacion", "denegado", "no me deja entrar",
				"iniciar sesion", "inicio de sesion", "login", "loguear", "autenticacion",
				"usuario bloqueado", "cuenta bloqueada", "desbloquear", "restablecer",
				"recuperar cuenta", "token", "mfa", "doble factor", "2fa", "credenciales",
				"rol", "perfil", "directorio activo", "active directory", "sso", "carpeta compartida")) {
			return CategoriaTicket.ACCESOS;
		}

		// 3. HARDWARE (Componentes físicos, periféricos y energía)
		if (contieneAlguna(texto,
				"pantalla", "monitor", "teclado", "mouse", "raton", "impresora", "escaner",
				"equipo", "computador", "computadora", "pc", "portatil", "laptop", "torre",
				"cpu", "disco duro", "ssd", "memoria ram", "bateria", "cargador", "cable",
				"fuente de poder", "no enciende", "no prende", "apagado", "se apaga",
				"sobrecalienta", "calienta", "ruido", "ventilador", "camara", "webcam",
				"microfono", "audifonos", "diadema", "parlantes", "usb", "puerto", "hdmi",
				"docking", "toner", "cartucho", "atasco de papel", "telefono fijo")) {
			return CategoriaTicket.HARDWARE;
		}

		// 4. SOFTWARE (Aplicaciones, sistemas operativos, ofimática y fallos lógicos)
		if (contieneAlguna(texto,
				"aplicacion", "aplicativo", "software", "sistema", "programa", "app",
				"error", "fallo", "bug", "excepcion", "pantallazo", "pantalla azul", "bsod",
				"se cierra", "crashea", "congelado", "se congela", "no responde", "lento",
				"instalar", "instalacion", "desinstalar", "actualizar", "actualizacion",
				"licencia", "activacion", "windows", "office", "excel", "word", "powerpoint",
				"outlook", "correo", "teams", "zoom", "navegador", "chrome", "edge",
				"erp", "crm", "sap", "base de datos", "archivo danado", "plugin", "complemento")) {
			return CategoriaTicket.SOFTWARE;
		}

		return CategoriaTicket.OTRO;
	}


	private PrioridadTicket clasificarPrioridad(String textoOriginal) {
		String texto = normalizar(textoOriginal);

		// 1. CRITICA (Parada total, impacto masivo, seguridad comprometida o producción caída)
		if (contieneAlguna(texto,
				"caido", "caida", "servicio caido", "sistema caido", "servidor caido",
				"bloqueado", "bloqueo total", "paralizado", "detenido", "no funciona nada",
				"sin servicio", "fuera de linea", "offline", "emergencia", "critico", "critica",
				"grave", "severo", "toda la empresa", "toda la oficina", "todos los usuarios",
				"varios usuarios", "equipo no enciende", "no prende", "pantallazo azul",
				"perdida de datos", "virus", "ransomware", "malware", "hackeo", "brecha",
				"vulnerabilidad", "fuga de informacion", "produccion detenida", "facturacion detenida")) {
			return PrioridadTicket.CRITICA;
		}

		// 2. ALTA (Impide trabajar a un usuario o tiene plazos/eventos inmediatos)
		if (contieneAlguna(texto,
				"urgente", "urgencia", "reunion", "presentacion", "cliente", "entrega",
				"fecha limite", "deadline", "hoy mismo", "en minutos", "cuanto antes",
				"asap", "prioritario", "inmediato", "inmediata", "no puedo trabajar",
				"impide trabajar", "me impide", "sin acceso", "cuenta bloqueada",
				"no me deja entrar", "no puedo conectarme", "sin internet", "fallo grave",
				"error critico", "cierre de mes", "nomina", "auditoria")) {
			return PrioridadTicket.ALTA;
		}

		// 3. BAJA (Consultas, solicitudes informativas, mejoras o tareas planificadas sin impacto operativo)
		if (contieneAlguna(texto,
				"consulta", "duda", "pregunta", "informacion", "sugerencia", "mejora",
				"cuando puedan", "sin afan", "sin prisa", "no es urgente", "baja prioridad",
				"capacitacion", "tutorial", "guia", "manual", "solicitud de cambio",
				"nuevo requerimiento", "cotizacion", "configuracion menor", "estetico",
				"color", "icono", "formato", "preventivo", "mantenimiento preventivo")) {
			return PrioridadTicket.BAJA;
		}

		// 4. MEDIA (Incidentes operativos estándar con solución alternativa o impacto moderado)
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
