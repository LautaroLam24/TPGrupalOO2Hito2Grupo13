package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository("pedidoRepository")
public interface IPedidoRepository extends JpaRepository<Pedido, Integer> {

    @Query("""
            select p
            from UnidadDeVenta u join u.pedidos p
            where u.id = :unidadId and p.id in :pedidoIds
            order by p.id
            """)
    List<Pedido> findDeUnidadByIds(
            @Param("unidadId") int unidadId,
            @Param("pedidoIds") Collection<Integer> pedidoIds);

    @Query("""
            select p.id, p.nombre, count(distinct pedido.id)
            from UnidadDeVenta u
            join u.pedidos pedido
            join pedido.items item
            join u.platos p
            where u.id = :unidadId
              and item.plato = p
              and pedido.fecha between :fechaInicio and :fechaFin
            group by p.id, p.nombre
            order by count(distinct pedido.id) desc,
                     lower(p.nombre) asc,
                     p.id asc
            """)
    List<Object[]> findRankingPlatosPorPedidos(
            @Param("unidadId") int unidadId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    @Query("""
            select sum(p.total)
            from Festival f
            join f.unidades u
            join u.pedidos p
            where f.id = :festivalId
              and p.fecha between :fechaInicio and :fechaFin
            """)
    Double sumTotalByFestivalAndFechas(
            @Param("festivalId") int festivalId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);

    @Query("""
            select u.id, coalesce(sum(p.total), 0)
            from Festival f
            join f.unidades u
            left join u.pedidos p
                on p.fecha between f.fechaInicio and f.fechaFin
            where f.id = :festivalId
            group by u.id
            """)
    List<Object[]> findRecaudacionPorUnidadDeFestival(@Param("festivalId") int festivalId);
}
