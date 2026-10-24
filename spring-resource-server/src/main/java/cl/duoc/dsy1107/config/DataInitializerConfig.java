package cl.duoc.dsy1107.config;

import cl.duoc.dsy1107.entity.Categoria;
import cl.duoc.dsy1107.entity.Producto;
import cl.duoc.dsy1107.repository.CategoriaRepository;
import cl.duoc.dsy1107.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializerConfig {

    @Bean
    public CommandLineRunner initDatabase(CategoriaRepository categoriaRepository,
                                          ProductoRepository productoRepository) {
        return args -> {
            if (productoRepository.count() == 0) {
                Categoria catComputacion = categoriaRepository.save(new Categoria(null, "Computación y Gaming"));
                Categoria catPerifericos = categoriaRepository.save(new Categoria(null, "Periféricos y Accesorios"));

                productoRepository.save(new Producto(null, "Laptop Gamer RTX 4070", 1250000.0, catComputacion));
                productoRepository.save(new Producto(null, "Mouse Inalámbrico Pro", 24990.0, catPerifericos));
                productoRepository.save(new Producto(null, "Teclado Mecánico RGB", 65990.0, catPerifericos));
                productoRepository.save(new Producto(null, "Monitor Curvo 27\" 165Hz", 219990.0, catComputacion));
            }
        };
    }
}
