package com.codedev.shofy.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Modelo para detalle completo de una compra/venta */
public class CompraDetalle implements Serializable {

    public static class ProductoLinea implements Serializable {
        private final String nombre;
        private final int cantidad;
        private final double precioUnitario;

        public ProductoLinea(String nombre, int cantidad, double precioUnitario) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }

        public String getNombre() { return nombre; }
        public int getCantidad() { return cantidad; }
        public double getPrecioUnitario() { return precioUnitario; }
    }

    private final int idVenta;
    private final long fechaMillis;   // ✅ epoch ms (UTC)
    private final String cliente;
    private final String correo;      // opcional
    private double total;
    private final List<ProductoLinea> productos = new ArrayList<>();

    /** Constructor compatible con el DBVentas que te pasé (sin correo). */
    public CompraDetalle(int idVenta, long fechaMillis, String cliente) {
        this(idVenta, fechaMillis, cliente, "");
    }

    /** Constructor con correo por si lo necesitas en otra parte. */
    public CompraDetalle(int idVenta, long fechaMillis, String cliente, String correo) {
        this.idVenta = idVenta;
        this.fechaMillis = fechaMillis;
        this.cliente = cliente != null ? cliente : "";
        this.correo = correo != null ? correo : "";
    }

    public void addProducto(String nombre, int cantidad, double precioUnitario) {
        productos.add(new ProductoLinea(nombre, cantidad, precioUnitario));
    }

    public int getIdVenta() { return idVenta; }
    public long getFechaMillis() { return fechaMillis; }
    public String getCliente() { return cliente; }
    public String getCorreo() { return correo; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public List<ProductoLinea> getProductos() { return productos; }
}
