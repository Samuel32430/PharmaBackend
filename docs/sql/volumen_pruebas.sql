-- Solo en desarrollo o pruebas. Nunca en producción.
-- Ejecutar después de la migración V4 (o tras arrancar el backend con ddl-auto=update).
-- Nota: si tu tabla productos fue creada por Hibernate (@JoinColumn(name = "categoria")),
-- cambia 'categoria' / 'categoria_id' según corresponda en tu esquema Oracle.
-- También verifica que Producto.fecha_modificacion no sea NOT NULL sin valor en el insert:
-- aquí incluimos fecha_modificacion para evitar ORA-01400 si la columna es NOT NULL.

insert into productos (nombre, precio, stock, estado, categoria,
                       fecha_creacion, fecha_modificacion, creado_por)
select 'Producto de prueba ' || lpad(level, 5, '0'),
       round(dbms_random.value(1, 200), 2),
       trunc(dbms_random.value(0, 500)),
       1,
       (select min(id) from categorias),
       systimestamp,
       systimestamp,
       'carga'
from dual connect by level <= 5000;

insert into ventas (fecha, total, estado, cliente_id, creado_por)
select systimestamp - numtodsinterval(dbms_random.value(0, 365), 'DAY'),
       round(dbms_random.value(5, 500), 2),
       'REGISTRADA',
       (select min(id) from clientes),
       'carga'
from dual connect by level <= 20000;

commit;
