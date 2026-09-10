package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO;
import pe.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO;
import pe.edu.upeu.PharmaBackend.enums.EstadoVenta;
import pe.edu.upeu.PharmaBackend.model.Venta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    @Query(
            """
            SELECT distinct v FROM Venta v
            LEFT JOIN FETCH v.cliente c
            LEFT JOIN FETCH v.detalles d
            LEFT JOIN FETCH d.producto p
            WHERE (:clienteId is null or c.id = :clienteId)
            and(:estado is null or v.estado = :estado)
            and(:desde is null or v.fecha >= : desde)
            and(:hasta is null or v.fecha <= : hasta)
            """
    )
    List<Venta> buscar(@Param("clienteId") Long clienteId,
                       @Param("estado")EstadoVenta estado,
                       @Param("desde")LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta,
                       Sort sort
                       );


    @Query("""
            select new pe.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO(
                       cat.id,
                       cat.nombre,
                       sum(d.cantidad),
                       sum(d.subtotal))
            from DetalleVenta d
            join d.venta v
            join d.producto p
            join p.categoria cat
            where v.estado = pe.edu.upeu.PharmaBackend.enums.EstadoVenta.REGISTRADA
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha <= :hasta)
            group by cat.id, cat.nombre
            order by sum(d.subtotal) desc
            """)
    List<VentaPorCategoriaDTO> reporteVentasPorCategoria(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);


    @Query("""
            select new pe.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO(
                       p.id,
                       p.nombre,
                       cat.nombre,
                       sum(d.cantidad),
                       sum(d.subtotal))
            from DetalleVenta d
            join d.venta v
            join d.producto p
            join p.categoria cat
            where v.estado = pe.edu.upeu.PharmaBackend.enums.EstadoVenta.REGISTRADA
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha <= :hasta)
            group by p.id, p.nombre, cat.nombre
            order by sum(d.cantidad) desc
            """)
    List<ProductoMasVendidoDTO> reporteProductosMasVendidos(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);



}
