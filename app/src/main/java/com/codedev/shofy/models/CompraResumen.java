package com.codedev.shofy.models;

import java.io.Serializable;

public class CompraResumen implements Serializable {
    private final int idVenta;
    private final int idUsuario;
    private final long fechaMillis;   // ✅ epoch millis (UTC)
    private final String cliente;
    private final double total;

    public CompraResumen(int idVenta, int idUsuario, long fechaMillis, String cliente, double total) {
        this.idVenta = idVenta;
        this.idUsuario = idUsuario;
        this.fechaMillis = fechaMillis;
        this.cliente = cliente != null ? cliente : "";
        this.total = total;
    }

    public int getIdVenta() { return idVenta; }
    public int getIdUsuario() { return idUsuario; }
    public long getFechaMillis() { return fechaMillis; }
    public String getCliente() { return cliente; }
    public double getTotal() { return total; }
}
