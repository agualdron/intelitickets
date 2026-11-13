ALTER TABLE tickets
	ADD CONSTRAINT chk_tickets_estado
		CHECK (estado IN ('ABIERTO', 'EN_PROCESO', 'RESUELTO', 'CERRADO', 'ELIMINADO')),
	ADD CONSTRAINT chk_tickets_categoria
		CHECK (categoria IN ('HARDWARE', 'SOFTWARE', 'CONECTIVIDAD', 'ACCESOS', 'OTRO')),
	ADD CONSTRAINT chk_tickets_prioridad
		CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA', 'CRITICA'));

ALTER TABLE historial_estados_ticket
	ADD CONSTRAINT chk_historial_estado_anterior
		CHECK (estado_anterior IN ('ABIERTO', 'EN_PROCESO', 'RESUELTO', 'CERRADO', 'ELIMINADO')),
	ADD CONSTRAINT chk_historial_estado_nuevo
		CHECK (estado_nuevo IN ('ABIERTO', 'EN_PROCESO', 'RESUELTO', 'CERRADO', 'ELIMINADO'));
