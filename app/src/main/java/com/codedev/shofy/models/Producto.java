package com.codedev.shofy.models;

import java.io.Serializable;

public class Producto implements Serializable {

    private int id;
    private String nombre;
    private String descripcion;
    private String tipo;
    private int cantidad_actual;
    private int cantidad_minima;
    private double precioBase;

    public Producto(int id, String nombre, String descripcion, String tipo, int cantidad_actual, int cantidad_minima, double precioBase) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.cantidad_actual = cantidad_actual;
        this.cantidad_minima = cantidad_minima;
        this.precioBase = precioBase;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }

    public void setCantidad_minima(int cantidad_minima) {
        this.cantidad_minima = cantidad_minima;
    }
    public void setPrecioBase(double precioBase) {
        this.precioBase = precioBase;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() { return tipo; }
    public int getCantidad_actual() { return cantidad_actual; }
    public int getCantidad_minima() { return cantidad_minima; }
    public double getPrecioBase() { return precioBase; }

    public void setNombre(String nombre) { this.nombre = nombre; }

    // ✅ NUEVO: Permite actualizar el stock internamente
    public void setCantidad_actual(int cantidad_actual) {
        this.cantidad_actual = cantidad_actual;
    }

    public void setDescripcion(String descripcion) {
    }
}
