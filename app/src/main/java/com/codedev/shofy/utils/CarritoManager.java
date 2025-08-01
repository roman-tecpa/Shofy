package com.codedev.shofy.utils;

import com.codedev.shofy.models.ItemCarrito;
import com.codedev.shofy.models.Producto;

import java.util.ArrayList;
import java.util.List;

public class CarritoManager {

    private static CarritoManager instancia;
    private final List<ItemCarrito> items;

    private CarritoManager() {
        items = new ArrayList<>();
    }

    public static CarritoManager getInstancia() {
        if (instancia == null) {
            instancia = new CarritoManager();
        }
        return instancia;
    }

    public List<ItemCarrito> getItems() {
        return items;
    }

    public void agregarProducto(Producto producto, int cantidad) {
        // Buscar si ya existe el producto en el carrito
        for (ItemCarrito item : items) {
            if (item.getProducto().getId() == producto.getId()) {
                // Actualizar cantidad sumando
                item.setCantidad(item.getCantidad() + cantidad);
                return;
            }
        }
        // Si no existe, agregar nuevo item
        items.add(new ItemCarrito(producto, cantidad));
    }

    public void eliminarProducto(Producto producto) {
        items.removeIf(item -> item.getProducto().getId() == producto.getId());
    }

    public void limpiarCarrito() {
        items.clear();
    }

    // 🔍 NUEVO: Obtener la cantidad que ya hay en el carrito de este producto
    public int obtenerCantidadDeProducto(Producto producto) {
        for (ItemCarrito item : items) {
            if (item.getProducto().getId() == producto.getId()) {
                return item.getCantidad();
            }
        }
        return 0;
    }
}
