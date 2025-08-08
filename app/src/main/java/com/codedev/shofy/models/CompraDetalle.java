package com.codedev.shofy.models;

import java.util.ArrayList;
import java.util.List;

/** Modelo para detalle completo de una compra/venta */
public class CompraDetalle {

    private int idVenta;
    private String fecha;
    private String cliente;
    private String correo;
    private double total;
    private List<ProductoLinea> productos;

    public CompraDetalle(int idVenta, String fecha, String cliente, String correo, double total) {
        this.idVenta = idVenta;
        this.fecha = fecha;
        this.cliente = cliente;
        this.correo = correo;
        this.total = total;
        this.productos = new ArrayList<>();
    }

    public int getIdVenta() {
        return idVenta;
    }

    public String getFecha() {
        return fecha;
    }

    public String getCliente() {
        return cliente;
    }

    public String getCorreo() {
        return correo;
    }

    public double getTotal() {
        return total;
    }

    public List<ProductoLinea> getProductos() {
        return productos;
    }

    /** Submodelo para cada línea del ticket */
    public static class ProductoLinea {
        private String nombre;
        private int cantidad;
        private double precioUnitario;

        public ProductoLinea(String nombre, int cantidad, double precioUnitario) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }

        public String getNombre() {
            return nombre;
        }

        public int getCantidad() {
            return cantidad;
        }

        public double getPrecioUnitario() {
            return precioUnitario;
        }
    }
}
