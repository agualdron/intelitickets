package com.empresa.tickets.ai;

import com.empresa.tickets.ai.dto.AnalisisTicketResponse;

public interface AnalizadorTicketService {

	AnalisisTicketResponse analizar(String titulo, String descripcion);
}
