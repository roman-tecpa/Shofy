package com.codedev.shofy.models;

import java.io.Serializable;

public class CompraResumen implements Serializable {
    private final int idVenta;
    private final String fechaVenta;
    private final double total;
    private final String productoEjemplo;     // 1 producto de la compra
    private final int productosDistintos;     // cuántos distintos hubo

    public CompraResumen(int idVenta, String fechaVenta, double total,
                         String productoEjemplo, int productosDistintos) {
        this.idVenta = idVenta;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.productoEjemplo = productoEjemplo;
        this.productosDistintos = productosDistintos;
    }

    public int getIdVenta() { return idVenta; }
    public String getFechaVenta() { return fechaVenta; }
    public double getTotal() { return total; }
    public String getProductoEjemplo() { return productoEjemplo; }
    public int getProductosDistintos() { return productosDistintos; }
}
