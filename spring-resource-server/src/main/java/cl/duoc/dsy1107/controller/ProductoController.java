package cl.duoc.dsy1107.controller;

import cl.duoc.dsy1107.entity.Categoria;
import cl.duoc.dsy1107.entity.Producto;
import cl.duoc.dsy1107.repository.CategoriaRepository;
import cl.duoc.dsy1107.repository.ProductoRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoController(ProductoRepository productoRepository,
                              CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping("/public/productos")
    public List<Producto> obtenerPublicos() {
        return productoRepository.findAll();
    }

    @GetMapping("/api/productos")
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    @GetMapping("/api/categorias")
    public List<Categoria> obtenerCategorias() {
        return categoriaRepository.findAll();
    }

    @PostMapping({"/api/productos", "/api/admin/productos"})
    public Producto crearProducto(@RequestBody Producto producto) {
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
        }
        if (producto.getPrecio() == null || producto.getPrecio() <= 0) {
            throw new IllegalArgumentException("El precio del producto debe ser mayor que cero");
        }
        return productoRepository.save(producto);
    }
}
