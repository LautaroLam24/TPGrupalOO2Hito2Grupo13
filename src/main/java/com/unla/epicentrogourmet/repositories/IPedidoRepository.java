package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
